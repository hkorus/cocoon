package org.ggp.base.player.gamer.statemachine;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import org.ggp.base.apps.player.detail.DetailPanel;
import org.ggp.base.apps.player.detail.SimpleDetailPanel;
import org.ggp.base.player.gamer.event.GamerSelectedMoveEvent;
import org.ggp.base.player.gamer.exception.GamePreviewException;
import org.ggp.base.player.gamer.statemachine.StateMachineGamer;
import org.ggp.base.util.game.Game;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.implementation.propnet.FirstPropNetStateMachine;
import org.ggp.base.util.statemachine.implementation.prover.ProverStateMachine;

import platypus.logging.PlatypusLogger;
import platypus.utils.StateSave;
import players.BryceMonteCarloTreeSearch;
import players.BryceMonteCarloTreeSearch_NoMiniMax;
import players.BryceMonteCarloTreeSearch_NoMiniMax_MultiThreaded;
import players.PlayerResult;
import players.TerminalStateProximity;
import players.WinCheckBoundedSearch;

public class PlatypusPlayer extends StateMachineGamer {

	private static final String PLAYER_NAME = "Platypus";

	private List<Move> optimalSequence = null;
	private PlayerResult playerResult = new PlayerResult();
	private TerminalStateProximity terminalStateProximity;
	
	private long propNetCreationTime;
	
	private List<StateMachine> stateMachines = new ArrayList<StateMachine>();

	// Optional second argument - level of logging. Default is ALL. Logs to
	// logs/platypus
	private static Logger log = PlatypusLogger.getLogger("game"
			+ System.currentTimeMillis());

	@Override
	public StateMachine getInitialStateMachine() {
		// TODO Auto-generated method stub
		//return getStateMachine
		long startTime = System.currentTimeMillis();
		StateMachine firstMachine = new FirstPropNetStateMachine();
		long stopTime = System.currentTimeMillis();
		propNetCreationTime = stopTime - startTime;
		System.out.println("PropNetCreationTime: " + propNetCreationTime);
		stateMachines.add(firstMachine);
		return firstMachine;
	}

	@Override
	public void stateMachineMetaGame(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException,
			GoalDefinitionException {

		//terminalStateProximity = new TerminalStateProximity(timeout - 1000,
		//		getStateMachine(), getCurrentState(), getRole(), log);

		if(propNetCreationTime==0) propNetCreationTime = 1;
		long estimatedThreadsToCreate = (timeout-System.currentTimeMillis())/propNetCreationTime;
		System.out.println("Estimating I can create " + estimatedThreadsToCreate + " propNets in given time.");
		//int MAX_NUM_THREADS = 4;
		for(int i=1; i<Math.min(BryceMonteCarloTreeSearch_NoMiniMax_MultiThreaded.MAX_NUM_THREADS,estimatedThreadsToCreate); i++){
			FirstPropNetStateMachine first = new FirstPropNetStateMachine();
			first.initialize(((FirstPropNetStateMachine) stateMachines.get(0)).getDescription());
			stateMachines.add(first);
			System.out.println("added new prop machine: " + i);
			if(System.currentTimeMillis()>timeout) break;
		}
	}

	@Override
	public Move stateMachineSelectMove(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException,
			GoalDefinitionException {
		long start = System.currentTimeMillis();
		playerResult.setBestMoveSoFar(null);

		FirstPropNetStateMachine stateMachine = (FirstPropNetStateMachine) getStateMachine();
		List<List<Move>> moves = stateMachine.getLegalMoves_Factoring(getCurrentState(),
				getRole());
		System.out.println("LegalMoves: " + moves);
		
		List<List<List<Move>>> jointMoves = stateMachine.getLegalJointMoves_Factoring(getCurrentState());
		System.out.println("Legal Joint Moves: " + jointMoves);
//		if (moves.size() == 1) {
//			Move bestMove = moves.get(0);
//			long stop = System.currentTimeMillis();
//			notifyObservers(new GamerSelectedMoveEvent(moves, bestMove, stop- start));
//			return bestMove;
//		}

		/* Allocate 10% of time to basic minimax */
		// Thread minimaxPlayerThread = new Thread(new MinimaxSubplayer)

//		Thread minimaxThread = new Thread(new WinCheckBoundedSearch(
//				getStateMachine(), getRole(), playerResult, getCurrentState(),
//				log));
//		minimaxThread.start();
//
//		try {
//			/* Sleep for 2 seconds less than the maximum time allowed */
//			Thread.sleep(2500);
//		} catch (InterruptedException e) {
//			//System.out.println("Done with subplayer!");
//			// e.printStackTrace();
//		}
		/* Tell the thread searching for the best move it is done so it can exit */
		//minimaxThread.interrupt();
//		Move sureMove = playerResult.getSureMove();
//		log.info("--------Best Move after Minimiax--------");
//		if (sureMove == null) {
//			log.info("sure move is null: Minimax did not result in anything");
//		}else {
//			log.info("Sure move is " + playerResult.sureMove);
//			log.info("with the sure score of " + playerResult.getSureScore());
//			
//			if (playerResult.sureScore == 100 || playerResult.getGameSolved()){
//				long stop = System.currentTimeMillis();
//				log.info("Game solved!");
//				log.info("Choosing move "+ playerResult.sureMove + " after minimax preliminary search");
//				notifyObservers(new GamerSelectedMoveEvent(moves.get(0), sureMove, stop- start));
//				return sureMove;
//			}
//		}
		

		Thread playerThread = new Thread(new BryceMonteCarloTreeSearch_NoMiniMax_MultiThreaded(
				getStateMachine(), getRole(), playerResult, getCurrentState(),
				log, stateMachines, timeout-3000));
		log.info("Starting Monte Carlo");
		playerThread.start();
		try {
			/* Sleep for 1 secondd less than the maximum time allowed */
			long sleeptime = timeout - System.currentTimeMillis() - 2000 - 1000;
			log.info("PAUSING PLATYPUS FOR " + sleeptime);
			Thread.sleep(sleeptime);
		} catch (InterruptedException e) {
			log.info("Done with subplayer!");
			// e.printStackTrace();
		}
		/* Tell the thread searching for the best move it is done so it can exit */
		//playerThread.interrupt();
		try {
			/* Sleep for 2 seconds less than the maximum time allowed */
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			log.info("Done with subplayer!");
			// e.printStackTrace();
		}
		Move bestMove = playerResult.getBestMoveSoFar();
		log.info("--------Best Move after Monte Carlo--------");
		if (bestMove == null) {
			bestMove = moves.get(0).get(new Random().nextInt(moves.get(0).size()));
			log.info("CHOSE RANDOM");
		}
		long stop = System.currentTimeMillis();
		log.info("best move: " + bestMove);
		notifyObservers(new GamerSelectedMoveEvent(moves.get(0), bestMove, stop
				- start));
		//log.info("Time left: " + timeout-)
		return bestMove;
	}

	@Override
	public void stateMachineStop() {
		// TODO Auto-generated method stub

	}

	@Override
	public void stateMachineAbort() {
		// TODO Auto-generated method stub

	}

//	@Override
//	public void analyze(Game g, long timeout) throws GameAnalysisException {
//		// TODO Auto-generated method stub
//
//	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return PLAYER_NAME;
	}

	@Override
	public DetailPanel getDetailPanel() {
		return new SimpleDetailPanel();
	}

	@Override
	public void preview(Game g, long timeout) throws GamePreviewException {
		// TODO Auto-generated method stub
		
	}

}