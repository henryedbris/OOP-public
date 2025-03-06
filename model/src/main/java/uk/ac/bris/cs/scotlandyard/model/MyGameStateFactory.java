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
		private static Set<Move.SingleMove> makeSingleMoves(GameSetup setup, List<Player> detectives, Player player, int source){
			HashSet<Move.SingleMove> moveHashSet = new HashSet<Move.SingleMove>();
			for(int destination : setup.graph.adjacentNodes(source)) {
				for (Player p : detectives) {
					if (p.location() != destination) {
						for (ScotlandYard.Transport t : setup.graph.edgeValueOrDefault(source, destination, ImmutableSet.of())) {
							if (player.has(t.requiredTicket())) {
								moveHashSet.add(new Move.SingleMove(player.piece(), source, t.requiredTicket(), destination));
							}
							if(player.isMrX() && player.has(ScotlandYard.Ticket.SECRET) && t.requiredTicket() != ScotlandYard.Ticket.SECRET){
								moveHashSet.add(new Move.SingleMove(player.piece(), source, ScotlandYard.Ticket.SECRET, destination));
							}
						}
					}
				}
			}
			return moveHashSet;
		}

		private static Set<Move.DoubleMove> makeDoubleMoves(GameSetup setup, List<Player> detectives, Player player, int source){
			HashSet<Move.DoubleMove> moveHashSet = new HashSet<Move.DoubleMove>();
			ScotlandYard.Ticket ticket1;
			ScotlandYard.Ticket ticket2;
			boolean isFerry1 = false;
			boolean isFerry2 = false;
			if (player.has(ScotlandYard.Ticket.DOUBLE)) {
				for(int destination1 : setup.graph.adjacentNodes(source)) {
					for (Player p : detectives) {
						if (p.location() != destination1) { //find valid destinations for move1
							for (ScotlandYard.Transport t : setup.graph.edgeValueOrDefault(source, destination1, ImmutableSet.of())) {
								if (player.has(t.requiredTicket())) {
									ticket1 = t.requiredTicket();
									if (ticket1 == ScotlandYard.Ticket.SECRET) isFerry1 = true;
									for(int destination2 : setup.graph.adjacentNodes(destination1)) {
										for (Player p2 : detectives) {
											if (p2.location() != destination2) {
												for (ScotlandYard.Transport t2 : setup.graph.edgeValueOrDefault(source, destination2, ImmutableSet.of())) {
													if (player.has(t2.requiredTicket())) {
														ticket2 = t2.requiredTicket();
														if (ticket2 == ScotlandYard.Ticket.SECRET) isFerry2 = true;
														moveHashSet.add(new Move.DoubleMove(player.piece(),source,ticket1,destination1,ticket2,destination2));
														if (player.has(ScotlandYard.Ticket.SECRET)) {
															if (player.hasAtLeast(ScotlandYard.Ticket.SECRET,2)){
																if (!isFerry1 && !isFerry2){
																	moveHashSet.add(new Move.DoubleMove(player.piece(),source, ScotlandYard.Ticket.SECRET,destination1, ScotlandYard.Ticket.SECRET,destination2));
																}
																if (isFerry1 && !isFerry2) moveHashSet.add(new Move.DoubleMove(player.piece(),source,ticket1,destination1, ScotlandYard.Ticket.SECRET,destination2));
																if (!isFerry1 && isFerry2) moveHashSet.add(new Move.DoubleMove(player.piece(),source, ScotlandYard.Ticket.SECRET,destination1,ticket2,destination2));
															}
															if (!isFerry1 && !isFerry2) {
																moveHashSet.add(new Move.DoubleMove(player.piece(), source, ScotlandYard.Ticket.SECRET, destination1, ticket2, destination2));
																moveHashSet.add(new Move.DoubleMove(player.piece(),source,ticket1,destination1, ScotlandYard.Ticket.SECRET,destination2));
															}
														}
													}
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
			return moveHashSet;
		}

		@Nonnull @Override public ImmutableSet<Move> getAvailableMoves() {
			HashSet<Move> moves = new HashSet<>();

			for(Player p: detectives) {
				moves.addAll(makeSingleMoves(setup, detectives, p, p.location()));
				moves.addAll(makeDoubleMoves(setup, detectives, p, p.location()));
			}
				moves.addAll(makeSingleMoves(setup, detectives, mrX, mrX.location()));
				moves.addAll(makeDoubleMoves(setup, detectives, mrX, mrX.location()));
			return ImmutableSet.copyOf(moves);
		}


		@Override public GameState advance(Move move) {
			return move.accept(new Move.Visitor<GameState>() {
				@Override
				public GameState visit(Move.SingleMove move) {
					// move.commencedBy();
					// if mr x do mr x things
					// if detective remove from remaining give ticket to mr x
					return null;
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
