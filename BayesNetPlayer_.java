import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class BayesNetPlayer_ extends NannonPlayer {
	// BayesNet: board --> move ---> win
	
	private class Board_TableMatch{
		int []config;
		public Board_TableMatch(int[] theConfig){
			config=theConfig;
		}
		@Override 
		public int hashCode(){
			return Arrays.hashCode(config);
		}
		@Override
		public boolean equals(Object obj){
			if(obj instanceof Board_TableMatch)
			{
				Board_TableMatch btm=(Board_TableMatch)obj;
				return Arrays.equals(config, btm.config);
			}
			return false;
		}
		@Override
		public String toString(){
			String out="";
			for(int i=0;i<config.length;i++){
				out+=config[i]+" ";
			}
			return out;
		}
	}
	private class Board_TableResult{
		Map<List<Integer>, Integer> moveCount;
		private int count;
		public Board_TableResult(){
			moveCount=new HashMap<List<Integer>, Integer>();
			count=0;
		}
		public void UpdateMove(List<Integer> theMove)
		{
			if(moveCount.containsKey(theMove)){
				int theCount=moveCount.get(theMove);
				moveCount.put(theMove, theCount+1);
			}
			else{
				moveCount.put(theMove, 1);
			}
			count++;
		}
		public Set<List<Integer>> getMoves(){
			return moveCount.keySet();
		}
		public double getProb(List<Integer> theMove){
			if(!moveCount.containsKey(theMove))
				return -1;
			int theCount=moveCount.get(theMove);
			return (double)theCount/(double)count;
		}
		public int getCount(){
			return count;
		}
		@Override
		public String toString(){
			String out="";
			for(Map.Entry<List<Integer>, Integer> entry: moveCount.entrySet())
			{
				List<Integer> move=entry.getKey();
				Integer count=entry.getValue();
				out+="Move: [";
				for(Integer i:move){
					out+=i+" ";
				}
				out+="], count: "+count+" ";
			}
			return out;
		}
	}
	private class Move_TableMatch{
		List<Integer> move;
		public Move_TableMatch(List<Integer> theMove){
			move=theMove;
		}
		@Override
		public int hashCode(){
			return move.hashCode();
		}
		@Override
		public boolean equals(Object obj){
			if(obj instanceof Move_TableMatch){
				Move_TableMatch mtm=(Move_TableMatch)obj;
				return move.equals(mtm.move);
			}
			return false;
		}
		public List<Integer> getMove(){
			return move;
		}
		@Override
		public String toString(){
			String out="";
			for(int i=0;i<move.size();i++){
				out+=move.get(i)+" ";
			}
			return out;
		}
	}
	private class Move_TableResult{
		private int win;
		private int loss;
		public Move_TableResult(){
			win=loss=0;
		}
		public void Update(boolean isWin){
			if(isWin) win++;
			else loss++;
		}
		public int getWin(){
			return win;
		}
		public int getLoss(){
			return loss;
		}
	}
	
	private Map<Board_TableMatch, Board_TableResult> board_to_move;
	private Map<Move_TableMatch, Move_TableResult> move_to_result;
	private int totalCount;
	private int totalWin;
	private int totalLoss;
	
	public BayesNetPlayer_(){
		board_to_move=new HashMap<Board_TableMatch, Board_TableResult>();
		move_to_result=new HashMap<Move_TableMatch, Move_TableResult>();
		totalCount=0;
		totalWin=0;
		totalLoss=0;
	}
	public BayesNetPlayer_(NannonGameBoard gb)	{
		super(gb);
		board_to_move=new HashMap<Board_TableMatch, Board_TableResult>();
		move_to_result=new HashMap<Move_TableMatch, Move_TableResult>();
		totalCount=0;
		totalWin=0;
		totalLoss=0;
	}
	@Override
	public String getPlayerName() {
		return "BayesNetPlayer_";
	}

	public double Prob_Win_Under_Config_Move(Board_TableMatch btm, Move_TableMatch mtm){
		// get P(config)
		Board_TableResult btr=board_to_move.get(btm);
		if(btr==null){
			return 0.5;
		}
		int btr_count=btr.getCount();
		double P_config=(double)btr_count/(double)totalCount;
		
		// get P(move|config)
		double P_move_with_config = btr.getProb(mtm.getMove());
		if(P_move_with_config<0){
			return 0.5;
		}
		// get P(win|move)
		Move_TableResult mtr=move_to_result.get(mtm);
		if(mtr==null){
			return 0.5;
		}
		int win=mtr.getWin();
		int loss=mtr.getLoss();
		double P_win_with_move=(double)win/(double)(win+loss);
		double P_loss_with_move=1-P_win_with_move;
		double a1=P_config*P_move_with_config*P_win_with_move;
		double a2=P_config*P_move_with_config*P_loss_with_move;
		return a1/(a1+a2);
	}
	
	@Override
	public List<Integer> chooseMove(int[] boardConfiguration,
			List<List<Integer>> legalMoves) {
		Board_TableMatch btm=new Board_TableMatch(boardConfiguration);
		double winProb=-1;
		List<Integer> theMove=null;
		for(List<Integer> move: legalMoves){
			Move_TableMatch mtm=new Move_TableMatch(move);
			double prob=Prob_Win_Under_Config_Move(btm, mtm);
			if(winProb<prob){
		//		System.out.println("winProb is "+winProb+", and prob is "+prob);
				winProb=prob;
				theMove=move;
			}
		}
		return theMove;
	}

	
	
	@Override
	public void updateStatistics(boolean didIwinThisGame,
			List<int[]> allBoardConfigurationsThisGameForPlayer,
			List<Integer> allCountsOfPossibleMovesForPlayer,
			List<List<Integer>> allMovesThisGameForPlayer) {
		for(int i=0;i<allMovesThisGameForPlayer.size();i++){
			int[] config=allBoardConfigurationsThisGameForPlayer.get(i);
			List<Integer> move=allMovesThisGameForPlayer.get(i);
			if(move==null){
				continue;
			}
			
			Board_TableMatch btm = new Board_TableMatch(config);
			if(board_to_move.containsKey(btm)){
				Board_TableResult btr=board_to_move.get(btm);
				btr.UpdateMove(move);
			}
			else{
				Board_TableResult btr=new Board_TableResult();
				btr.UpdateMove(move);
				board_to_move.put(btm, btr);
			}
			
			Move_TableMatch mtm = new Move_TableMatch(move);
			if(move_to_result.containsKey(mtm)){
				Move_TableResult mtr=move_to_result.get(mtm);
				mtr.Update(didIwinThisGame);
			}
			else{
				Move_TableResult mtr = new Move_TableResult();
				mtr.Update(didIwinThisGame);
				move_to_result.put(mtm, mtr);
			}			
			totalCount++;
			if(didIwinThisGame){
				totalWin++;
			}
			else{
				totalLoss++;
			}
		}

	}

	@Override
	public void reportLearnedModel() {
		double maxRatio=-1;
		Board_TableMatch theBTM=null;
		Move_TableMatch theMTM=null;
		for(Map.Entry<Board_TableMatch, Board_TableResult> entry: board_to_move.entrySet())
		{
			Board_TableMatch btm=entry.getKey();
			Board_TableResult btr=entry.getValue();
			for(List<Integer> move: btr.getMoves()){
				Move_TableMatch mtm=new Move_TableMatch(move);
				Move_TableResult mtr=move_to_result.get(mtm);
				if(mtr==null){
					System.out.println("mtr is null, this should not happen.");
					System.exit(-1);
				}
				// get P(config);
				int btr_count=btr.getCount();
				double P_config=(double)btr_count/(double)totalCount;
				
				// get P(move|config)
				double P_move_with_config = btr.getProb(mtm.getMove());
				// get P(win|move)
				int win=mtr.getWin();
				int loss=mtr.getLoss();
				if(loss==0){
					win++;
					loss++;
				}
				double P_win_with_move=(double)win/(double)(win+loss);
				double P_loss_with_move=1-P_win_with_move;
				double a1=P_config*P_move_with_config*P_win_with_move;
				double a2=P_config*P_move_with_config*P_loss_with_move;
				if(a1/a2>maxRatio){
					maxRatio=a1/a2;
					theBTM = btm;
					theMTM = mtm;
				}		
			}
		}
		System.out.println("The variables with the max ratio are: configs: "+theBTM+" moves: "+theMTM+", and the max ratio is "+maxRatio/( (double)totalWin / (double)totalLoss ));
	}

}
