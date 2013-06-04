package players;

import java.util.HashMap;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;

public class PlayerResult {
	public boolean gameSolved;
	public Move sureMove;
	public double sureScore;
	public int minimaxDepth;

	public boolean gameSolved_temp;
	public Move bestMoveSoFar;
	public double bestMoveScore;
	public HashMap<MachineState, Integer> memoizedMachineStateGoals = new HashMap<MachineState, Integer>();
	public HashMap<MachineState, Double> memoizedMachineStates = new HashMap<MachineState, Double>();

	public PlayerResult() {
	}

	public synchronized void setBestMoveSoFar(Move move) {
		bestMoveSoFar = move;
	}

	public synchronized Move getBestMoveSoFar() {
		return bestMoveSoFar;
	}

	public synchronized void putMemoizedState(MachineState state, Double value) {
		memoizedMachineStates.put(state, value);
	}

	public synchronized Double getMemoizedState(MachineState state) {
		return memoizedMachineStates.get(state);
	}

	public synchronized boolean containsMemoizedState(MachineState state) {
		return memoizedMachineStates.containsKey(state);
	}

	public synchronized void setBestMoveScore(double score) {
		bestMoveScore = score;
	}

	public synchronized double getBestMoveScore() {
		return bestMoveScore;
	}

	public synchronized int getMemoizedStateGoal(MachineState state) {
		return memoizedMachineStateGoals.get(state);
	}

	public synchronized void putMemoizedStateGoal(MachineState state, int goal) {
		memoizedMachineStateGoals.put(state, goal);
	}

	public synchronized boolean containsMemoizedStateGoal(MachineState state) {
		return memoizedMachineStateGoals.containsKey(state);
	}

	public synchronized Move getSureMove() {
		return sureMove;
	}

	public synchronized void setSureMove(Move sureMove) {
		this.sureMove = sureMove;
	}

	public synchronized double getSureScore() {
		return sureScore;
	}

	public synchronized void setSureScore(double score) {
		this.sureScore = score;
	}

	public synchronized void setGameSolved(boolean gameSolved) {
		this.gameSolved = gameSolved;
	}

	public synchronized boolean getGameSolved() {
		return gameSolved;
	}

	public synchronized void reset() {
		gameSolved = false;
		sureMove = null;
		sureScore = Double.MIN_VALUE;
		minimaxDepth = 0;
		
		gameSolved_temp = false;
		bestMoveSoFar = null;
		bestMoveScore = Double.MIN_VALUE;
	}

	public String toString(){
		StringBuilder sb = new StringBuilder();
		if (gameSolved){
			sb.append("Solved, on depth "+ minimaxDepth+" move " + sureMove + " ("+sureScore+")");
		}else {
			sb.append("Not solved (depth "+minimaxDepth+"), the best move is: " + bestMoveSoFar + "("+bestMoveScore+")");
		}
		return sb.toString();
	}
}