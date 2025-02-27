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
				final List<Player> detectives)
		{
			this.setup = setup;
			this.remaining = remaining;
			this.log = log;
			this.mrX = mrX;
			this.detectives = detectives;
			if(setup.moves.isEmpty()) throw new IllegalArgumentException("Moves is empty!");
			if(remaining.isEmpty()) throw new IllegalArgumentException("Remaining is empty!");
			if(detectives.isEmpty()) throw new IllegalArgumentException("Detectives is empty!");
			if(!mrX.isMrX()) throw new IllegalArgumentException("No MrX");
			String[] colours = new String[]{"","","","",""};
			for(Player p : detectives){
				for(String colour:colours){
					if(p.piece().webColour().equals(colour)){
						throw new IllegalArgumentException("Duplicate detective");
					}else{
						colour = p.piece().webColour();
					}
				}
			}

		}


		@Override public GameSetup getSetup() {  return setup; }

		@Override public ImmutableSet<Piece> getPlayers() { return null; }

		@Nonnull @Override public Optional<Integer> getDetectiveLocation(Piece.Detective detective) {
			return Optional.empty();
		}

		@Nonnull @Override public Optional<TicketBoard> getPlayerTickets(Piece piece) {
			if(getPlayers().contains(piece)){
				String colour = piece.webColour();



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
