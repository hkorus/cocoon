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
	private int NUM_PLAYERS = 1;

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
		stateMachines.clear();
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
		//create one propnet to get a time estimate

		long startTime = System.currentTimeMillis();
		FirstPropNetStateMachine first = new FirstPropNetStateMachine();
		first.initialize(((FirstPropNetStateMachine) stateMachines.get(0)).getDescription());
		long duration = System.currentTimeMillis() - startTime;
		System.out.println("Took " + duration + " to initialize propnet #" + 1);
		stateMachines.add(first);
		System.out.println("added new prop machine: " + 1);
		propNetCreationTime = System.currentTimeMillis() - startTime;

		if(propNetCreationTime==0) propNetCreationTime = 1;
		long estimatedThreadsToCreate = (timeout-System.currentTimeMillis())/propNetCreationTime;
		System.out.println("Estimating I can create " + estimatedThreadsToCreate + " propNets in given time.");

		for(int i=2; i<Math.min(BryceMonteCarloTreeSearch_NoMiniMax_MultiThreaded.MAX_NUM_THREADS,estimatedThreadsToCreate); i++){
			first = new FirstPropNetStateMachine();
			startTime = System.currentTimeMillis();
			first.initialize(((FirstPropNetStateMachine) stateMachines.get(0)).getDescription());
			duration = System.currentTimeMillis() - startTime;
			System.out.println("Took " + duration + " to initialize propnet #" + i);
			stateMachines.add(first);
			System.out.println("added new prop machine: " + i);
			if(System.currentTimeMillis()>timeout) break;
		}
		System.out.println("Finished making propnets.");
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


		/* Check if montecarlo is a good idea */
		long depthStartTime = System.currentTimeMillis();
		stateMachine.performDepthCharge(getCurrentState(), null);
		long depthStopTime = System.currentTimeMillis();
		
		long netTime = depthStopTime-depthStartTime;
		System.out.println("Depth charge Time: " + netTime);
		if(netTime*50>(timeout-System.currentTimeMillis()-2500)){
			
			/*MonteCarlo is a bad idea.  Just avoid losing. */
			List<Move> allLegalMoves = stateMachine.getLegalMoves(getCurrentState(), getRole());
			NUM_PLAYERS = getStateMachine().getRoles().size();
			Move bestMove = allLegalMoves.get(new Random().nextInt(allLegalMoves.size()));
			for(Move move : allLegalMoves){
				boolean moveIsBad = false;
				for(List<Move> allJointMoves : stateMachine.getLegalJointMoves(getCurrentState(),getRole(),move)){
					if(isSuicidal(getCurrentState(),move,stateMachine.getNextState(getCurrentState(),allJointMoves))){
						moveIsBad= true; break;
					}
				}
				if(!moveIsBad) bestMove = move;
			}
			long stop = System.currentTimeMillis();
			log.info("Monte Carlo Deemed Bad Idea");
			log.info("best move: " + bestMove);
			notifyObservers(new GamerSelectedMoveEvent(moves.get(0), bestMove, stop
					- start));
			return bestMove;
		}
		
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
	
	
	private boolean isSuicidal(MachineState currentState, Move move, MachineState acquiredState) {
		StateMachine stateMachine = getStateMachine();
		if(NUM_PLAYERS==1) return false;
		try {
			if (!stateMachine.isTerminal(acquiredState)) {
				List<Move> legalMoves = stateMachine.getLegalMoves(acquiredState, getRole());
				for (Move nextMove : legalMoves) {
					List<List<Move>> jointMoves = stateMachine.getLegalJointMoves(acquiredState, getRole(), nextMove);

					for(List<Move> jointMove : jointMoves){
						MachineState nextState = stateMachine.getNextState(acquiredState, jointMove);
						
						if ((stateMachine.isTerminal(nextState) && stateMachine.getGoal(nextState,getRole())==0)) {
							System.out.println("Decided that move " + move + " was SUICIDAL");
							return true;
						}
					}
				}
				
			}
		} catch (MoveDefinitionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransitionDefinitionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (GoalDefinitionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return false;
	}

	@Override
	public void stateMachineStop() {
		// TODO Auto-generated method stub
		System.gc();
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