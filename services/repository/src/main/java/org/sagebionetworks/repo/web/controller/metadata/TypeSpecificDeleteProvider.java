package org.sagebionetworks.repo.web.controller.metadata;

import org.sagebionetworks.repo.model.Entity;

/**
 * Allows entity specific post delete actions
 * 
 * @param <T>
 */
public interface TypeSpecificDeleteProvider<T extends Entity> extends EntityProvider<T> {
	
	/**
	 * Called when an entity is deleted.
	 * @param deleted 
	 */
	public void entityDeleted(T deleted);

}
