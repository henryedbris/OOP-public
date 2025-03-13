package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableSet;
import com.google.common.graph.ImmutableNetwork;
import uk.ac.bris.cs.scotlandyard.model.Board.GameState;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;

import java.util.*;
import java.util.stream.Collectors;

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
			// check if each detective is a valid detective
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
			for (Player d : detectives){
				pieces.add(d.piece());
			}
			pieces.add(mrX.piece());
			return ImmutableSet.copyOf(pieces);
		}

		@Nonnull @Override public Optional<Integer> getDetectiveLocation(Piece.Detective detective) {
			for (Player d : detectives) {
				if (d.piece().equals(detective)) return Optional.of(d.location());
			}
			return Optional.empty();
		}

		@Nonnull @Override public Optional<TicketBoard> getPlayerTickets(Piece piece) {
			Player player = null;
			// find the player the piece belongs to
			if(getPlayers().contains(piece)){
				if(piece.isMrX()) player = mrX;
				for(Player d : detectives){
					if(d.piece().equals(piece)){
						 player = d;
					}
				}
				Player finalPlayer = player;
				// find the number of tickers the player has
				TicketBoard ticketBoard = ticket -> {
                    int counter = 0;
                    while(finalPlayer.hasAtLeast(ticket,counter)){
                        counter ++;
                    }
                    counter --;
                    return counter;
                };
				return Optional.of(ticketBoard);
			}
			return Optional.empty();
		}

		@Nonnull @Override public ImmutableList<LogEntry> getMrXTravelLog() {
			return log;
		}

		// check if given piece has moves left
		private boolean hasMoves (Piece piece){
			for (Move m : getAvailableMoves()){
				if (m.commencedBy().equals(piece)) return true;
			}
			return false;
		}

		// check if given location matches any detectives location
		private boolean equalsDetectivesLocation (int location){
			for (Player d : detectives){
				if (d.location() == location) return true;
			}
			return false;
		}
		// check if given player has any tickets left
		private boolean hasTickets(Player player) {
			for (ScotlandYard.Ticket ticket : ScotlandYard.Ticket.values()) {
				if (player.hasAtLeast(ticket, 1)) {
					return true;
				}
			}
			return false;
		}

		@Nonnull @Override public ImmutableSet<Piece> getWinner() {
			boolean mrxWins = false;
			boolean detectivesWins = false;

			// mrx wins if log is full and detectives have no more valid moves mrx wins
			boolean detectivesCanMove = false;
			if (getMrXTravelLog().size() == getSetup().moves.size()){
				for (Player d : detectives){
					if (hasMoves(d.piece())) detectivesCanMove = true;
					break;
				}
				if (!detectivesCanMove) mrxWins = true;
			}

			// if all detectives have no tickets left then mrx wins
			boolean detectivesHasTickets = false;
			for (Player d : detectives){
				if (hasTickets(d)) detectivesHasTickets = true;
			}
			if (!detectivesHasTickets) mrxWins = true;

			// detectives wins if mrx is found or mrx has no more available moves
			if (equalsDetectivesLocation(mrX.location()) || !hasMoves(mrX.piece())) detectivesWins = true;

			// if winner is found then end game
			if (detectivesWins || mrxWins) remaining = ImmutableSet.of();

			if (mrxWins) return ImmutableSet.of(mrX.piece());
			if (detectivesWins) return ImmutableSet.copyOf(detectives.stream().map(Player::piece).toList());
			else return ImmutableSet.of();
		}

		private Set<Move.SingleMove> makeSingleMoves(GameSetup setup, List<Player> detectives, Player player, int source){
			HashSet<Move.SingleMove> moveHashSet = new HashSet<Move.SingleMove>();
			for(int destination : setup.graph.adjacentNodes(source)) {
				// find valid destinations from the source where there is no detective
				boolean detectiveFound = equalsDetectivesLocation(destination);
                if (!detectiveFound){
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
			return moveHashSet;
		}

		private Set<Move.DoubleMove> makeDoubleMoves(GameSetup setup, List<Player> detectives, Player player, int source){
			HashSet<Move.DoubleMove> moveHashSet = new HashSet<Move.DoubleMove>();
			// ensure there are enough moves left to perform a double move
			int movesLeft = getSetup().moves.size() - getMrXTravelLog().size();
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
			// get available moves for the current rounds players
			for (Piece r : remaining){
				for(Player d : detectives){
					if (d.piece() == r)moves.addAll(makeSingleMoves(setup,detectives,d, d.location()));
				}
				if (r == mrX.piece()){
					moves.addAll(makeSingleMoves(setup, detectives, mrX, mrX.location()));
					moves.addAll(makeDoubleMoves(setup, detectives, mrX, mrX.location()));
				}
			}
			return ImmutableSet.copyOf(moves);
		}

		// check if the round is a hidden or reveal round and update log accordingly
		private LogEntry newLogEntry (int turn, ScotlandYard.Ticket ticket,int location){
			if (getSetup().moves.get(turn)){
				return LogEntry.reveal(ticket, location);
			}else{
				return LogEntry.hidden(ticket);
			}
		}

		@Override public GameState advance(Move move) {
			int turn = getMrXTravelLog().size();
			ImmutableSet<Move> moves = getAvailableMoves();
			if(!moves.contains(move)) throw new IllegalArgumentException("Illegal move: "+move);

			return move.accept(new Move.Visitor<GameState>() {

				@Override
				public GameState visit(Move.SingleMove move) {
					if (move.commencedBy() == mrX.piece()){
						LogEntry newEntry = newLogEntry(turn,move.ticket,move.destination);
						//update mrx's location and use ticket
						mrX = mrX.at(move.destination).use(move.ticket);
						// update log
						log = ImmutableList.<LogEntry>builder()
								.addAll(log)
								.add(newEntry)
								.build();
						// swap to detectives turn
						remaining = ImmutableSet.copyOf(detectives.stream()
								.map(Player::piece)
								.collect(Collectors.toSet()));
					}
					else {
						List<Player> updatedDetectives = new ArrayList<>();
						// find the current turns detective
						for (Player d : detectives) {
							if (move.commencedBy() == d.piece()){
								// update detective location, use ticket and give ticket to mrx
								updatedDetectives.add(d.at(move.destination).use(move.ticket));
								mrX = mrX.give(move.ticket);
							}else updatedDetectives.add(d);
						}
						detectives = updatedDetectives;
						// remove detective from remaining and remove detectives who cannot move
						remaining = remaining.stream()
								.filter(piece -> hasMoves(piece))
								.filter(piece -> !piece.equals(move.commencedBy()))
								.collect(ImmutableSet.toImmutableSet());
						// swap to mrX turn if all detectives have moved
						if (remaining.isEmpty()){
							remaining = ImmutableSet.of(mrX.piece());
						}
					}
					return new MyGameState(getSetup(), remaining, log, mrX, detectives);
				}
				@Override
				public GameState visit(Move.DoubleMove move) {
					// update log with first move
					LogEntry firstEntry = newLogEntry(turn,move.ticket1, move.destination1);
					// update log with second move
					LogEntry secondEntry = newLogEntry(turn + 1,move.ticket2,move.destination2);
					mrX = mrX.at(move.destination2).use(move.tickets());
					log = ImmutableList.<LogEntry>builder()
							.addAll(log)
							.add(firstEntry)
							.add(secondEntry)
							.build();

					// swap to detectives turn
					remaining = ImmutableSet.copyOf(detectives.stream()
							.map(Player::piece)
							.collect(Collectors.toSet()));

					return new MyGameState(getSetup(), remaining, log, mrX, detectives);
				}
			});
		}
	}
	@Nonnull @Override public GameState build(
			GameSetup setup,
			Player mrX,
			ImmutableList<Player> detectives) {
		return new MyGameState(setup, ImmutableSet.of(Piece.MrX.MRX), ImmutableList.of(), mrX, detectives);
	}



}
