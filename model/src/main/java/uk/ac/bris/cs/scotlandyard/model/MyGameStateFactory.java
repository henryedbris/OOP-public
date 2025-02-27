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
			if(getPlayers().contains(piece)){
				String colour = piece.webColour();
				for(Player p : detectives){
					if(colour.equals(p.piece().webColour())){
						Player player = p;
					}
				}

				TicketBoard x = new TicketBoard() {
					@Override public int getCount(@Nonnull ScotlandYard.Ticket ticket) {
						return 0;
					}
				};

				return Optional.of(x);


			}
			return Optional.empty();
		}

		@Nonnull @Override public ImmutableList<LogEntry> getMrXTravelLog() {
			return log;
		}

		@Nonnull @Override public ImmutableSet<Piece> getWinner() {
			return null;
		}

		@Nonnull @Override public ImmutableSet<Move> getAvailableMoves() {
			return null;
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
