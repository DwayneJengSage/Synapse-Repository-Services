package org.sagebionetworks.repo.web.controller.metadata;

import java.util.List;

import org.sagebionetworks.repo.manager.table.ColumnModelManager;
import org.sagebionetworks.repo.manager.table.TableRowManager;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.dao.table.TableRowTruthDAO;
import org.sagebionetworks.repo.model.table.TableEntity;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Validation for TableEntities.
 * 
 * @author John
 *
 */
public class TableEntityMetadataProvider implements EntityValidator<TableEntity>, TypeSpecificDeleteProvider<TableEntity> {
	
	@Autowired
	ColumnModelManager columnModelManager;

	@Autowired
	TableRowManager tableRowManager;

	@Override
	public void validateEntity(TableEntity entity, EntityEvent event) throws InvalidModelException, NotFoundException,
			DatastoreException, UnauthorizedException {
		// For create/update/new version we need to bind the columns to the entity.
		if(EventType.CREATE == event.getType() || EventType.UPDATE == event.getType() || EventType.NEW_VERSION == event.getType()){
			boolean isNew = false;
			if(EventType.CREATE == event.getType()){
				isNew = true;
			}
			List<String> columnIds = entity.getColumnIds();
			// Bind the entity to these columns
			columnModelManager.bindColumnToObject(event.getUserInfo(), columnIds, entity.getId(), isNew);
		}	
	}

	@Override
	public void entityDeleted(TableEntity deleted) {
		tableRowManager.deleteAllRows(deleted.getId());
		columnModelManager.unbindAllColumnsAndOwnerFromObject(deleted.getId());
	}
}
