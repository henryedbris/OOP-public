package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;

import java.util.*;

/**
 * cw-model
 * Stage 1: Complete this class
 */
public final class MyGameStateFactory implements Factory<GameState> {

	private final class MyGameState implements GameState {

		private GameSetup setup;
		private ImmutableSet<Piece> remaining;
		private ImmutableList<LogEntry> log;
		private Player mrX;
		private List<Player> detectives;
		private ImmutableSet<Move> moves;
		private ImmutableSet<Piece> winner;
		private MyGameState(
				final GameSetup setup,
				final ImmutableSet<Piece> remaining,
				final ImmutableList<LogEntry> log,
				final Player mrX,
				final List<Player> detectives) {
			this.setup = setup;
			this.remaining = remaining;
			this.log = log;
			this.mrX = mrX;
			this.detectives = detectives;

			if(setup.moves.isEmpty()) throw new IllegalArgumentException("Moves is empty!");
			if(remaining.isEmpty()) throw new IllegalArgumentException("Remaining is empty!");
			if(!mrX.isMrX()) throw new IllegalArgumentException("No MrX");
			if(detectives.isEmpty()) throw new IllegalArgumentException("Detectives is empty!");
			String[] colours = new String[detectives.size()];
			int[] locations = new int[detectives.size()];
			for(Player p : detectives){
				for(int i = 0; i < colours.length; i++){
					if(p.piece().webColour().equals(colours[i])){
						throw new IllegalArgumentException("Duplicate detectives");
					}else{
						colours[i] = p.piece().webColour();
					}
					if (p.location()==locations[i]){
						throw new IllegalArgumentException("Duplicate detective location");
					}else{
						locations[i] = p.location();
					}
				}
				if (p.has(ScotlandYard.Ticket.SECRET)) throw new IllegalArgumentException("Detective has secret");
				if (p.has(ScotlandYard.Ticket.DOUBLE)) throw new IllegalArgumentException("Detective has double");
			}
			if (setup.graph.nodes().isEmpty()) throw new IllegalArgumentException("Graph is empty!");
		}

		@Override public GameSetup getSetup() {  return setup; }

		@Override public ImmutableSet<Piece> getPlayers() {
			List<Piece> pieces = new ArrayList<>();
			for (Player p : detectives){
				pieces.add(p.piece());
			}
			pieces.add(mrX.piece());
			return ImmutableSet.copyOf(pieces);
		}

		@Nonnull @Override public Optional<Integer> getDetectiveLocation(Piece.Detective detective) {
			for (Player p : detectives) {
				if (p.piece().equals(detective)) return Optional.of(p.location());
			}
			return Optional.empty();
		}

		@Nonnull @Override public Optional<TicketBoard> getPlayerTickets(Piece piece) {
			Player player = null;
			if(getPlayers().contains(piece)){
				if(piece.isMrX()) player = mrX;
				for(Player p : detectives){
					if(p.piece().equals(piece)){
						 player = p;
					}
				}
				Player finalPlayer = player;
				TicketBoard ticketBoard = ticket -> {
                    int counter = 0;
                    while(finalPlayer.hasAtLeast(ticket,counter)){
                        counter = counter + 1;
                    }
                    counter = counter - 1;
                    return counter;
                };
				return Optional.of(ticketBoard);
			}
			return Optional.empty();
		}

		@Nonnull @Override public ImmutableList<LogEntry> getMrXTravelLog() {
			return log;
		}

		@Nonnull @Override public ImmutableSet<Piece> getWinner() {
			return null;
		}
		private Set<Move.SingleMove> makeSingleMoves(GameSetup setup, List<Player> detectives, Player player, int source){
			HashSet<Move.SingleMove> moveHashSet = new HashSet<Move.SingleMove>();
			// find valid destinations from the source where there is no detective
			for(int destination : setup.graph.adjacentNodes(source)) {
				for (Player p : detectives) {
					if (p.location() != destination) {
						// find all possible tickets the player can use to move to the destination
						for (ScotlandYard.Transport t : Objects.requireNonNull(setup.graph.edgeValueOrDefault(source, destination, ImmutableSet.of()))) {
							if (player.has(t.requiredTicket())) {
								moveHashSet.add(new Move.SingleMove(player.piece(), source, t.requiredTicket(), destination));
							}
							// use secret ticket if MrX has a secret ticket
							if(player.isMrX() && player.has(ScotlandYard.Ticket.SECRET)){
								moveHashSet.add(new Move.SingleMove(player.piece(), source, ScotlandYard.Ticket.SECRET, destination));
							}
						}
					}
				}
			}
			return moveHashSet;
		}

		private Set<Move.DoubleMove> makeDoubleMoves(GameSetup setup, List<Player> detectives, Player player, int source){
			HashSet<Move.DoubleMove> moveHashSet = new HashSet<Move.DoubleMove>();
			// ensure there are enough moves left to perform a double move
			int movesLeft = setup.moves.size() - getMrXTravelLog().size();
			if (player.has(ScotlandYard.Ticket.DOUBLE) && movesLeft >= 2){
				// create a set of all possible first moves from the source
				Set<Move.SingleMove> ticket1Set = makeSingleMoves(setup, detectives, player, source);
				for(Move.SingleMove move1 : ticket1Set){
					int destination1 = move1.destination;
					// find valid second moves from the destination of move1
					Set<Move.SingleMove> ticket2Set = makeSingleMoves(setup, detectives, player, destination1);
					for(Move.SingleMove move2 : ticket2Set){
						// ensure the player has enough tickets to perform the double move
						if((move1.ticket == move2.ticket) && (player.hasAtLeast(move1.ticket,2))){
							moveHashSet.add(new Move.DoubleMove(player.piece(), source, move1.ticket, destination1, move2.ticket, move2.destination));}
						if(move1.ticket != move2.ticket){
							moveHashSet.add(new Move.DoubleMove(player.piece(), source, move1.ticket, destination1, move2.ticket, move2.destination));}
					}
				}
			}
			return moveHashSet;
		}

		@Nonnull @Override public ImmutableSet<Move> getAvailableMoves() {
			HashSet<Move> moves = new HashSet<>();
//			for(Player p : detectives){
//				moves.addAll(makeSingleMoves(setup,detectives,p, p.location()));
//			}
				moves.addAll(makeSingleMoves(setup, detectives, mrX, mrX.location()));
				moves.addAll(makeDoubleMoves(setup, detectives, mrX, mrX.location()));
			return ImmutableSet.copyOf(moves);
		}

		@Override public GameState advance(Move move) {
			ImmutableSet<Move> moves = getAvailableMoves();
			if(!moves.contains(move)) throw new IllegalArgumentException("Illegal move: "+move);
			return move.accept(new Move.Visitor<GameState>() {

				@Override
				public GameState visit(Move.SingleMove move) {
					if(move.commencedBy() == mrX.piece()){
						mrX = mrX.use(move.ticket);
						mrX = mrX.at(move.destination);
						// this works for decreasing the ticket number and that is all
					}
					else{
						for(Piece p : remaining){
							if(p == move.commencedBy()){
								for(Player player : detectives){
									if(player.piece()==p){
										player = player.use(move.ticket);
//										need to somehow return a new mygamestate because gamestate is absract
//										and then change remaining to not have detective who just moved in it 
									}
								}
							}
						}
					}
					return new MyGameState(setup, ImmutableSet.of(Piece.MrX.MRX), ImmutableList.of(), mrX, detectives);
				}

				@Override
				public GameState visit(Move.DoubleMove move) {
					// travel log - update twice
					// detectives are remaining
					return null;
				}
			});
		}
	}
	@Nonnull @Override public GameState build(
			GameSetup setup,
			Player mrX,
			ImmutableList<Player> detectives) {
		return new MyGameState(setup, ImmutableSet.of(Piece.MrX.MRX), ImmutableList.of(), mrX, detectives);
//		 TODO
//		throw new RuntimeException("Implement me!");
	}



}
