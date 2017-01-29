import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


public class FullJointProbTablePlayer_ extends NannonPlayer {
	// Table:   current board configuration, move, move effect, win/loss
	private class TableResult{

		private int win;
		private int loss;
		public TableResult(){
			win=0;
			loss=0;
		}
		public void Update(boolean isWin){
			if (isWin) win++;
			else loss++;
		}
		public double WinOverLoss(){
			if(loss==0)
				return (double)(win+1)/(double)(loss+1);  // smooth
			return 
					(double)win/(double)loss;
		}
		public int getWin(){ return win; }
		public int getLoss(){ return loss;}
	}
	
	private class TableMatch{
		private int []config;
		private List<Integer> move;
		public TableMatch(int []theConfig, List<Integer> theMove){
			config=theConfig;
			move=theMove;
		}
		@Override
		public boolean equals(Object tm){
			if(tm instanceof TableMatch){
				TableMatch theTM=(TableMatch) tm;
				return Arrays.equals(config, theTM.config) && move.equals(theTM.move);
			}
			return false;
		}
		
		@Override 
		public int hashCode(){
			return Arrays.hashCode(config) + move.hashCode();
		}
		public String toString(){
			String out = "configs: ";
			for(int i=0;i<config.length;i++){
				out+=config[i]+" ";
			}
			out+=", moves: ";
			for(int i=0;i<move.size();i++){
				out+=move.get(i).toString()+" ";
			}
			return out;
		}
	}
	private Map<TableMatch, TableResult> probTable;
	
	public FullJointProbTablePlayer_(){
		probTable=new HashMap<TableMatch, TableResult>();
	}
	public FullJointProbTablePlayer_(NannonGameBoard gb){
		super(gb);
		probTable=new HashMap<TableMatch, TableResult>();
	}
	
	@Override
	public String getPlayerName() {
		return "FullJointProbTablePlayer_";
	}

	@Override
	public List<Integer> chooseMove(int[] boardConfiguration,
			List<List<Integer>> legalMoves) {
		double maxProb=-1;
		List<Integer> theMove=null;
		for(List<Integer> move: legalMoves){
			double matchProb=1;
			
			TableMatch tm=new TableMatch(boardConfiguration, move);
			if(probTable.containsKey(tm)){
				TableResult tr=probTable.get(tm);
				matchProb=tr.WinOverLoss();
			}
			if(matchProb>maxProb){
				maxProb=matchProb;
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
	//	System.out.printf("configuration#: %d, Move#:%d\n", allBoardConfigurationsThisGameForPlayer.size(), allMovesThisGameForPlayer.size());
		for(int i=0;i<allMovesThisGameForPlayer.size();i++){
			int[] config=allBoardConfigurationsThisGameForPlayer.get(i);
			List<Integer> move=allMovesThisGameForPlayer.get(i);
			if(move==null) continue;
			TableMatch tm=new TableMatch(config, move);
			if(probTable.containsKey(tm)){
				TableResult tr=probTable.get(tm);
				tr.Update(didIwinThisGame);
//				System.out.println("Find Existing Items in probTable.");
			}
			else{
				TableResult tr=new TableResult();
				tr.Update(didIwinThisGame);
				probTable.put(tm, tr);
			}
		}
		//System.out.println("size of progTable is "+probTable.size());
	}

	@Override
	public void reportLearnedModel() {
		//	prob( randomVariable(s) | win)  /  prob(randomVariable(s) | loss)= 
		// = prob(v, w)/ prob(v, l) / (p(win)/ p(loss))
		int win=0;
		int loss=0;
		double maxRatio=-1;
		TableMatch theTM=null;
		
		for(Map.Entry<TableMatch, TableResult> entry: probTable.entrySet())
		{
			TableMatch tm=entry.getKey();
			TableResult tr=entry.getValue();
			win+=tr.getWin();
			loss+=tr.getLoss();
			if(tr.WinOverLoss()>maxRatio)
			{
				maxRatio=tr.WinOverLoss();
				theTM=tm;
			}
		}
		if(loss==0) {
			win++;
			loss++;
		}
		String out="Variables with max raio: ";
		out+=theTM.toString();
		out+=", and the ratio is ";
		out+=maxRatio/ ((double)win/(double)loss);		
		System.out.println(out);
	}

}
