package players;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class TerminalStateProximity {
	private static final double COUNTS_TIMEREMAINING = 500;
	private static final double MONTECARLO_TIMEREMAINING = 1000+COUNTS_TIMEREMAINING;
	private StateMachine stateMachine;
	private MachineState initialState;
	private Map<GdlSentence, Double> terminalSentenceCounts = new HashMap<GdlSentence, Double>();
	private int numberTerminalStates;
	private int numberGdlSentences;
	private Role role;
	private Logger log;

	/* @param timeout the system time by which it must be done */
	public TerminalStateProximity(long timeout, StateMachine stateMachine, MachineState initialState, Role role, Logger log) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException{
		this.stateMachine = stateMachine;
		this.initialState = initialState;
		this.role = role;
		this.log = log;

		/* temporary */
		long terminationTimeLimit = timeout-(long)MONTECARLO_TIMEREMAINING;
		Set<MachineState> randomTerminalStates = generateRandomTerminalStates(terminationTimeLimit);

		numberTerminalStates = randomTerminalStates.size();

		/* temporary */
		terminalSentenceCounts = generateTerminalSentenceCounts(randomTerminalStates);
	}


	/**
	 * Uses Monte-Carlo simulation to generate random states until it finds terminal ones; starts from initialstate at each point
	 * Uses the goal value of each terminal state rescaled to the range -1 to 1 to add to the "value" of each state
	 * @param finishTime the time at which this method stops generating random terminal states
	 * @return a set containing randomly generate terminal state
	 * @throws TransitionDefinitionException 
	 * @throws MoveDefinitionException 
	 * @throws GoalDefinitionException 
	 */
	private Set<MachineState> generateRandomTerminalStates(long finishTime) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException{
		Set<MachineState> terminalStates = new HashSet<MachineState>();
		while(System.currentTimeMillis()<finishTime){
			MachineState currentState = initialState;
			while(!stateMachine.isTerminal(currentState) && System.currentTimeMillis()<finishTime){
				currentState = stateMachine.getRandomNextState(currentState);
			}

			//System.out.println(stateMachine.getGoal(currentState,role));
			if(System.currentTimeMillis()<finishTime)
				terminalStates.add(currentState);
		}
		System.out.println("Found " + terminalStates.size() + " terminal states");
		return terminalStates;
	}

	/**
	 * For each sentence that appears in a terminal state, counts the number of times it appears in the given terminal states
	 * @param terminalStates sample of terminal states to count GDL sentences in
	 * @return a map from GDL sentences to the number of times they occur in the terminal states
	 * @throws GoalDefinitionException 
	 */
	private Map<GdlSentence,Double> generateTerminalSentenceCounts(Set<MachineState> terminalStates) throws GoalDefinitionException{
		Map<GdlSentence, Double> sentenceCounts = new HashMap<GdlSentence,Double>();
		for(MachineState state : terminalStates){
			int goal = stateMachine.getGoal(state, role);
			for(GdlSentence sentence : state.getContents()){
				if(sentenceCounts.containsKey(sentence)){
					sentenceCounts.put(sentence, sentenceCounts.get(sentence)+(goal-50.0)/50);
				} else{
					sentenceCounts.put(sentence, (goal-50.0)/50);
				}
			}
		}
		numberGdlSentences = sentenceCounts.keySet().size();
		System.out.println("Found " + numberGdlSentences + " GDL sentences");
		return sentenceCounts;
	}

	/**
	 * @param state the state to evaluate
	 * @return a real number from 0 to 100 indicating the value of a given state, with 100 being most valuable
	 */
	public double evaluateState(MachineState state){

		double heuristic = 0;
		for(GdlSentence sentence : state.getContents()){
			if(terminalSentenceCounts.containsKey(sentence))
				heuristic+=terminalSentenceCounts.get(sentence)*100/(double)numberTerminalStates;
		}
		//if(heuristic!=0)
		//System.out.println(heuristic);
		return heuristic;
	}

}
