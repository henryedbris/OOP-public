package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;

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
			// Observers -set
			// Gamestate
			@Override
			public Board getCurrentBoard() {
				return null;
			}

			@Override
			public void registerObserver(@Nonnull Observer observer) {
				// add to set
			}

			@Override
			public void unregisterObserver(@Nonnull Observer observer) {
				// remove from set
			}

			@Nonnull
			@Override
			public ImmutableSet<Observer> getObservers() {
				// set into immutable set
				return null;
			}

			@Override
			public void chooseMove(@Nonnull Move move) {
				// advance
				// move from user interface
			}
		};
	}
}
