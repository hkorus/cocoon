package org.ggp.base.util.statemachine.implementation.propnet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlRelation;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.gdl.grammar.GdlTerm;
import org.ggp.base.util.propnet.architecture.Component;
import org.ggp.base.util.propnet.architecture.PropNet;
import org.ggp.base.util.propnet.architecture.components.*;
import org.ggp.base.util.propnet.factory.OptimizingPropNetFactory;
import org.ggp.base.util.propnet.factory.PropNetFactory;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.implementation.prover.query.ProverQueryBuilder;

import java.util.Collection;


@SuppressWarnings("unused")
public class FirstPropNetStateMachine extends StateMachine {
	/** The underlying proposition network  */
	private PropNet propNet;
	/** The topological ordering of the propositions */
	private List<Proposition> ordering;
	/** The player roles */
	private List<Role> roles;

	private List<Gdl> description;

	public List<Gdl> getDescription(){
		return description;
	}

	private List<Collection<Proposition>> factorList;

	private Move noopMove = null;


	/**
	 * Returns a move that is effectively a nooping move (has no effect on outcome of game), if one exists.  Returns null otherwise.
	 * @param role
	 * @return
	 */
	@Override
	public Move getNoopMove(){
		return noopMove;
	}

	/**
	 * Initializes the PropNetStateMachine. You should compute the topological
	 * ordering here. Additionally you may compute the initial state here, at
	 * your discretion.
	 */
	@Override
	public   void initialize(List<Gdl> description) {
		try{
			this.description = description;
			propNet = OptimizingPropNetFactory.create(description);
			//propNet.renderToFile("LastGamePlayedPropNet" + System.currentTimeMillis() + "_" + this.hashCode() + ".dot");
			roles = propNet.getRoles();
			ordering = getOrdering(propNet);
			for(Proposition p : ordering){
				//System.out.println(p.getName());
			}
			//Set<Set<Component>> factors = factorPropNet();
			System.out.println("PropNetStateMachine: starting to factor...");
			factorList = factorPropNet3();

			noopMove = findNoopMove(factorList);

			System.out.println("Noop Move: " + noopMove);

			System.out.println("Found "+factorList.size()+" factors.");
			for(Collection<Proposition> factor : factorList){
				System.out.println("Factor " + factor);
			}

		}catch(InterruptedException ex){
			ex.printStackTrace();
		}
	}

	private Move findNoopMove(List<Collection<Proposition>> factorList){
		for(Component p : getInputPropositions()){
			boolean foundPropInFactor = false;
			for(Collection<Proposition> factor : factorList){
				if(factor.contains(p)) foundPropInFactor = true;
			}
			if(!foundPropInFactor){
				return getMoveFromProposition((Proposition)p);
			}
		}
		return null;
	}

	private boolean propMarkConjunction(Component p, boolean isRecur){
		for (Component c : p.getInputs()){
			if(!propMarkP(c, isRecur)){
				return false;
			}
		}
		return true;
	}

	private boolean propMarkDisjunction(Component p, boolean isRecur){
		for (Component c : p.getInputs()){
			if(propMarkP(c, isRecur)){
				return true;
			}
		}
		return false;
	}





	private boolean propMarkPNonRecursive(Component p){
		return propMarkP(p, false);
	}

	private boolean propMarkPRecursive(Component p){
		return propMarkP(p, true);
	}

	private boolean propMarkP(Component p, boolean isRecur){
		//if(p == null) return false; //for bad legals in mini propnets

		if(p instanceof Proposition){ //should return false when reaching init?
			Proposition prop = (Proposition)p;
			if(isBase(prop) || isInput(prop) || prop == propNet.getInitProposition()){
				return prop.getValue();
			}else{
				if(!isRecur){
					return p.getValue();
				}else{
					if(p.getInputs().size() == 0){
						System.out.println("Returning false: "+((Proposition)p).getName());
						return false; // more legals!!!!
					}
					return propMarkP(p.getSingleInput(), isRecur);
				}
			}
		}else if (p instanceof Constant){
			return p.getValue();
		}else if (p instanceof And){
			return propMarkConjunction(p, isRecur);
		}else if (p instanceof Not){
			return !propMarkP(p.getSingleInput(), isRecur);
		}else if (p instanceof Or){
			return propMarkDisjunction(p, isRecur);
		}
		return false;
	}

	private void clearPropNet(){
		for(Proposition p : propNet.getPropositions()){
			p.setValue(false);
		}
	}

	private void markBases(MachineState state){
		Set<GdlSentence> sentences = state.getContents();
		Map<GdlSentence, Proposition> map = propNet.getBasePropositions();
		for(GdlSentence s : sentences){
			Proposition c = map.get(s);
			if(c!=null){
				c.setValue(true);
			}
		}
	}
	/**
	 * Computes if the state is terminal. Should return the value
	 * of the terminal proposition for the state.
	 */
	@Override
	public   boolean isTerminal(MachineState state) {
		markBases(state);
		boolean result = propMarkPRecursive(propNet.getTerminalProposition());
		clearPropNet();
		return result;
	}


	/**
	 * Attempts to factor the propnet into sets of independent moves.  Move independence is determined by which branch of the final Or
	 * from the terminal node the input is located in.  Returns a list with a single list of all input propositions of no factors are found.
	 * @return
	 */
	private List<Collection<Proposition>> factorPropNet3(){
		List<Collection<Proposition>> factorList = new ArrayList<Collection<Proposition>>();

		Component bottomOr = findBottomOr(propNet.getTerminalProposition());

		if(bottomOr==null){
			System.out.println("Could not find bottom Or so unfactorable.");
			/* Game is unfactorable, so return a single list containing all input propositions */
			List<Proposition> finalList = new ArrayList<Proposition>();
			finalList.addAll(propNet.getInputPropositions().values());

			factorList.add(finalList);
			return factorList;

		} else {
			System.out.println("Found bottom Or with " + bottomOr.getInputs().size() + " inputs.");
			/* Game is factorable, so create a new list of input propositions for each branch from the bottom Or. */
			for(Component parent : bottomOr.getInputs()){
				System.out.println("factoring beginning with " + parent);
				Collection<Proposition> factorInputs = new HashSet<Proposition>();
				Set<Component> visitedComponents = new HashSet<Component>();
				getFactorInputs(parent, factorInputs, visitedComponents);

				/* Check to see if this is a valid factor by seeing if it's base propositions overlap with any existing factors
				 * and whether it has any inputs at all.
				 */
				boolean factorIsValid = true;
				if(factorInputs.size()>0){
					for(Proposition p : factorInputs){
						for(int i=0; i< factorList.size();i++){
							if(factorList.get(i).contains(p)) {
								factorIsValid = false; break;
							}
						}
						if(factorIsValid==false) break;
					}

					if(!factorIsValid){
						System.out.println("Found invalid factor; ignoring.");
					} else {
						System.out.println("Added new factor.");
						factorList.add(factorInputs);
					}
				} else {
					System.out.println("Ignoring factor of size 0.");
				}
			}
			return factorList;
		}
	}

	/**
	 * Adds all input propositions to the provided set, searching recursively upwards from the supplied base proposition.
	 * @param visitedComponents a set to let it keep track of which components it has visited so far; used to deal with loops
	 *                          that occur in some propNets (MultipleButtonsAndLights, Connect4, etc).
	 */
	private void getFactorInputs(Component base, Collection<Proposition> factorInputs, Set<Component> visitedComponents){
		if(visitedComponents.contains(base)) return;
		visitedComponents.add(base);

		if(isInput(base)) {
			factorInputs.add((Proposition)base);
			return;
		}

		for(Component parent : base.getInputs()){
			getFactorInputs(parent, factorInputs, visitedComponents);
		}
	}



	/**
	 * Explores tree until it finds AND or OR component, returning the component if an Or is found, null otherwise
	 * @param terminalComp place to start in propnet
	 * @return null if and was found, an Or component otherwise
	 */
	private Component findBottomOr(Component terminalComp){
		Component currentComp = terminalComp;
		while(true){
			if(currentComp instanceof Or){
				return currentComp;
			} else if(currentComp instanceof And){
				return null;
			}

			/* No branching at all in this game */
			if(currentComp.getInputs().size()==0) return null;
			currentComp = currentComp.getSingleInput();
		}
	}


	/**
	 * Computes the goal for a role in the current state.
	 * Should return the value of the goal proposition that
	 * is true for that role. If there is not exactly one goal
	 * proposition true for that role, then you should throw a
	 * GoalDefinitionException because the goal is ill-defined. 
	 */
	@Override
	public   int getGoal(MachineState state, Role role)
			throws GoalDefinitionException {
		markBases(state);
		Set<Proposition> goalProps = propNet.getGoalPropositions().get(role);
		boolean found = false;
		Proposition goal = null;
		for(Proposition p : goalProps){
			if(propMarkPRecursive(p)){
				if(found) {
					clearPropNet();
					throw new GoalDefinitionException(state, role);
				}
				found = true;
				goal = p;
			}
		}
		if(!found) {
			clearPropNet();
			throw new GoalDefinitionException(state, role);
		}
		int val = getGoalValue(goal);
		//System.out.println("Goal value: "+val);
		clearPropNet();
		return val;
	}

	/**
	 * Returns the initial state. The initial state can be computed
	 * by only setting the truth value of the INIT proposition to true,
	 * and then computing the resulting state.
	 */
	@Override
	public   MachineState getInitialState() {
		propNet.getInitProposition().setValue(true);
		for(Proposition p: ordering){
			p.setValue(propMarkPNonRecursive(p.getSingleInput()));
		}
		MachineState state = getStateFromBase();
		clearPropNet();
		return state;
	}

	/**
	 * Computes the legal moves for role in state.
	 */
	@Override
	public   List<Move> getLegalMoves(MachineState state, Role role)
			throws MoveDefinitionException {

		//.out.println("Getting Legals for "+state.toString()+" and Role: "+role.toString());
		List<Move> listMoves = new LinkedList<Move>();
		markBases(state);
		Set<Proposition> legals = propNet.getLegalPropositions().get(role);
		if (legals == null) {
			return new ArrayList<Move>();
		}
		for(Proposition legal: legals){
			if(propMarkPRecursive(legal)){
				listMoves.add(getMoveFromProposition(legal));
			}
		}
		//System.out.println("Legals: "+listMoves.size());
		clearPropNet();
		return listMoves;
	}

	/**
	 * Returns legal moves divided into lists for each factor the game found; factors without any moves are discarded.
	 * List of factors is ordered, so the first factor always pertains to the same factor.
	 * @param state
	 * @param role
	 * @return
	 */
	public   List<List<Move>> getLegalMoves_Factoring(MachineState state, Role role){
		List<List<Move>> listMoves = new ArrayList<List<Move>>();
		markBases(state);
		Set<Proposition> legals = propNet.getLegalPropositions().get(role);

		if (legals == null) {
			return new ArrayList<List<Move>>();
		}

		for(Collection<Proposition> factor : factorList){
			List<Move> factorLegalMoves = new LinkedList<Move>();
			for(Proposition p : factor){
				Proposition legalP = propNet.getLegalInputMap().get(p);
				if(propMarkPRecursive(legalP) && legals.contains(legalP)) factorLegalMoves.add(getMoveFromProposition(legalP));
			}

			/* For games that alternate turns, we throw in a fake noop move */
			if(factorLegalMoves.isEmpty()){
				factorLegalMoves.add(getFakeNoopMove());
			}
			listMoves.add(factorLegalMoves);
		}
		clearPropNet();

		//System.out.println("Role: " + role + " has " + listMoves);
		return listMoves;
	}


	//private Move fakeNoopMove = getMoveFromProposition(new GdlSentence("noop"));
	public Move getFakeNoopMove(){
		return getNoopMove();
	}
	public int getNumFactors(){
		return factorList.size();
	}

	/**
	 * Returns all combinations of legal moves for each factor.  Needs to be updated to allow the user to tell it which factor
	 * it should process to save time.
	 * @param state
	 * @return
	 * @throws MoveDefinitionException
	 */
	public List<List<List<Move>>> getLegalJointMoves_Factoring(MachineState state) throws MoveDefinitionException {
		List<List<List<Move>>> jointMoves = new ArrayList<List<List<Move>>>();
		/* Initialize the jointMoves list */
		for(int i=0; i< getNumFactors(); i++){
			List<List<Move>> legals = new ArrayList<List<Move>>();
			for (Role r : getRoles()) {
				legals.add(getLegalMoves_Factoring(state, r).get(i));
			}

			List<List<Move>> crossProduct = new ArrayList<List<Move>>();
			crossProductLegalMoves(legals, crossProduct, new LinkedList<Move>());
			jointMoves.add(crossProduct);
		}
		return jointMoves;
	}
	
	//pass in factor to play on
		//get next state for factor
		public synchronized MachineState getRandomNextStateFromFactor(MachineState state, List<Move> projectedJointMove, int factorIndex) throws TransitionDefinitionException, MoveDefinitionException {

			for (int moveIndex = 0; moveIndex < projectedJointMove.size(); moveIndex++) {
				if (projectedJointMove.get(moveIndex) == getFakeNoopMove()) {
					projectedJointMove.set(moveIndex, getActualMove(state, factorIndex, moveIndex));
				}
			}

			return getNextState(state, projectedJointMove);

		}


		//search through legalJointMoves for all factors != factorIndex
		private Move getActualMove(MachineState state, int factorIndex, int moveIndex) throws MoveDefinitionException {

			List<List<List<Move>>> legalJointMoves = getLegalJointMoves_Factoring(state);
			for (int factor = 0; factor < getNumFactors(); factor++) {
				if (factor != factorIndex) {

					for (List<Move> jointMove : legalJointMoves.get(factor)) {
						if (jointMove.get(moveIndex) != getFakeNoopMove()) {
							return jointMove.get(moveIndex);
						}
					}

				}
			}
			return getFakeNoopMove();

		}


	/**
	 * Computes the next state given state and the list of moves.
	 */
	@Override
	public   MachineState getNextState(MachineState state, List<Move> moves)
			throws TransitionDefinitionException {
		//(moves.toString() + " "+state.toString());
		if(moves == null) {
			System.out.println("Moves is null :(");
			System.out.println(state);
			return state; //not sure exactly what this should be
		}

		List<GdlSentence> sentences = toDoes(moves);

		markActions(sentences);
		markBases(state);

		//HashMap<Proposition, Boolean> next = new HashMap<Proposition, Boolean>();
		for(Proposition p: ordering){
			p.setValue(propMarkPNonRecursive(p.getSingleInput()));

		}
		//for(Proposition p: next.keySet()){
		//	p.setValue(next.get(p));
		//}
		MachineState nextState = getStateFromBase();
		//if(nextState)
		//System.out.println("Next State Contents:"+nextState.getContents().toString());
		clearPropNet();
		String stateString = nextState.getContents().toString();
		return nextState;
	}

	private void markActions(List<GdlSentence> sentences){
		Map<GdlSentence, Proposition> inputs = propNet.getInputPropositions();
		for(GdlSentence sentence: sentences){
			Proposition c = inputs.get(sentence);
			if(c!=null) c.setValue(true);
		}
	}

	Set<Component> basePropositions = null;

	private boolean isBase(Component base){

		if(basePropositions!=null){
			return basePropositions.contains(base);
		}
		basePropositions = new HashSet<Component>(propNet.getBasePropositions().values());
		return basePropositions.contains(base);
	}

	private Set<Component> getBasePropositions(){
		if(basePropositions!=null){
			return basePropositions;
		}
		basePropositions = new HashSet<Component>(propNet.getBasePropositions().values());
		return basePropositions;
	}

	Set<Component> inputPropositions = null;

	private boolean isInput(Component base){
		return getInputPropositions().contains(base);
	}

	private Set<Component> getInputPropositions(){
		if(inputPropositions!=null) return inputPropositions;
		inputPropositions = new HashSet<Component>(propNet.getInputPropositions().values());
		return inputPropositions;
	}


	private List<Component> getLeaves(PropNet prop){
		List<Component> leaves = new LinkedList<Component>();
		leaves.addAll(prop.getBasePropositions().values());
		for(Component c: prop.getComponents()){

			if(c.getInputs().size() == 0){
				leaves.add(c);
			}
		}
		return leaves;
	}

	private boolean seenLink(List<Link>seenLinks, Component src, Component dst){
		for(Link link : seenLinks){
			if(link.dest == dst && link.source == src){
				return true;
			}
		}
		return false;
	}
	private boolean allInputsSeen(Component comp, List<Link> seenLinks){
		Set<Component> inputs = comp.getInputs();
		for(Component input: inputs){
			boolean found = false;
			if(!seenLink(seenLinks, input, comp)){
				return false;
			}
		}
		return true;
	}

	private class Link{
		Component source;
		Component dest;

		public Link(Component source, Component dest){
			this.source = source;
			this.dest = dest;
		}

	}
	/**
	 * This should compute the topological ordering of propositions.
	 * Each component is either a proposition, logical gate, or transition.
	 * Logical gates and transitions only have propositions as inputs.
	 * 
	 * The base propositions and input propositions should always be exempt
	 * from this ordering.
	 * 
	 * The base propositions values are set from the MachineState that
	 * operations are performed on and the input propositions are set from
	 * the Moves that operations are performed on as well (if any).
	 * 
	 * @return The order in which the truth values of propositions need to be set.
	 */

	public   List<Proposition> getOrdering(PropNet prop)
	{
		// List to contain the topological ordering.
		List<Proposition> order = new LinkedList<Proposition>();

		// All of the components in the PropNet
		List<Component> components = new ArrayList<Component>(prop.getComponents());

		// All of the propositions in the PropNet.		
		List<Component> noIncoming = getLeaves(prop);
		List<Link> seenLinks = new LinkedList<Link>();

		while(noIncoming.size() > 0){
			Component node = noIncoming.remove(0);

			if(node instanceof Proposition && !isBase(node) && !isInput(node) && node != prop.getInitProposition()){
				order.add((Proposition)node);
			}
			Set<Component> outputs = node.getOutputs();
			for(Component comp : outputs){
				if(seenLink(seenLinks, node, comp)){
					continue;
				}
				//mark the link
				Link link = new Link(node, comp);
				seenLinks.add(link);

				if(allInputsSeen(comp, seenLinks)){
					noIncoming.add(comp);
				}
			}

		}
		return order;

	}

	/* Already implemented for you */
	@Override
	public   List<Role> getRoles() {
		return roles;
	}

	/* Helper methods */

	/**
	 * The Input propositions are indexed by (does ?player ?action).
	 * 
	 * This translates a list of Moves (backed by a sentence that is simply ?action)
	 * into GdlSentences that can be used to get Propositions from inputPropositions.
	 * and accordingly set their values etc.  This is a naive implementation when coupled with 
	 * setting input values, feel free to change this for a more efficient implementation.
	 * 
	 * @param moves
	 * @return
	 */
	private List<GdlSentence> toDoes(List<Move> moves)
	{
		List<GdlSentence> doeses = new ArrayList<GdlSentence>(moves.size());
		Map<Role, Integer> roleIndices = getRoleIndices();

		for (int i = 0; i < roles.size(); i++)
		{
			int index = roleIndices.get(roles.get(i));
			doeses.add(ProverQueryBuilder.toDoes(roles.get(i), moves.get(index)));
		}
		return doeses;
	}

	/**
	 * Takes in a Legal Proposition and returns the appropriate corresponding Move
	 * @param p
	 * @return a PropNetMove
	 */
	public   static Move getMoveFromProposition(Proposition p)
	{
		return new Move(p.getName().get(1));
	}

	/**
	 * Helper method for parsing the value of a goal proposition
	 * @param goalProposition
	 * @return the integer value of the goal proposition
	 */	
	private int getGoalValue(Proposition goalProposition)
	{
		GdlRelation relation = (GdlRelation) goalProposition.getName();
		GdlConstant constant = (GdlConstant) relation.get(1);
		return Integer.parseInt(constant.toString());
	}

	/**
	 * A Naive implementation that computes a PropNetMachineState
	 * from the true BasePropositions.  This is correct but slower than more advanced implementations
	 * You need not use this method!
	 * @return PropNetMachineState
	 */	
	public   MachineState getStateFromBase()
	{
		Set<GdlSentence> contents = new HashSet<GdlSentence>();
		for (Proposition p : propNet.getBasePropositions().values())
		{

			p.setValue(p.getSingleInput().getValue());
			if (p.getValue())
			{
				contents.add(p.getName());
			}

		}
		return new MachineState(contents);
	}
}