package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;
import java.util.*;

import com.google.common.collect.ImmutableSet;
import uk.ac.bris.cs.scotlandyard.model.ScotlandYard.Factory;

/**
 * cw-model
 * Stage 2: Complete this class
 */
public final class MyModelFactory implements Factory<Model> {

	@Nonnull @Override public Model build(GameSetup setup,
	                                      Player mrX,
	                                      ImmutableList<Player> detectives) {

		// TODO
		return new Model() {
			@Nonnull
			// Observers set and Gamestate declared as private
			private Set<Model.Observer> observers = new HashSet<>();
			private Board.GameState gameState;

			@Override
			public Board getCurrentBoard() {
				return gameState;
			}

			@Override
			public void registerObserver(@Nonnull Observer observer) {
				//throw error if registering the same observer or null, otherwise adding it to set
				if (observers.contains(observer)) throw new IllegalArgumentException("Observer already registered");
				else if (observer.toString() == null) throw new IllegalArgumentException("Observer cannot be null");
				else observers.add(observer);
			}

			@Override
			public void unregisterObserver(@Nonnull Observer observer) {
				//throw error if unregistering observer not in set or null, otherwise removing it from set
				if (observer.toString() == null) throw new IllegalArgumentException("Observer cannot be null");
				else if (!observers.contains(observer)) throw new IllegalArgumentException("Observer already registered");
				else observers.remove(observer);
			}

			@Nonnull
			@Override
			public ImmutableSet<Observer> getObservers() {
				// converting set into immutable set
				return observers.stream().collect(ImmutableSet.toImmutableSet());
			}

			@Override
			public void chooseMove(@Nonnull Move move) {
				// advance
				// move from user interface
					for (Observer observer : observers) {
						if (!gameState.advance(move).getWinner().isEmpty()) {
							observer.onModelChanged(gameState, Observer.Event.GAME_OVER);
						}
						else observer.onModelChanged(gameState, Observer.Event.MOVE_MADE);
					}

				}



		};
	}
}
