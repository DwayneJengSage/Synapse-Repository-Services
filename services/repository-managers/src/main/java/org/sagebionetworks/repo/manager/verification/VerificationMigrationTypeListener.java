package org.sagebionetworks.repo.manager.verification;

import java.util.List;

import org.sagebionetworks.repo.manager.migration.MigrationTypeListener;
import org.sagebionetworks.repo.model.dao.NotificationEmailDAO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.DatabaseObject;
import org.sagebionetworks.repo.model.dbo.persistence.DBOVerificationSubmission;
import org.sagebionetworks.repo.model.dbo.verification.VerificationDAOImpl;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.verification.VerificationSubmission;
import org.springframework.beans.factory.annotation.Autowired;

public class VerificationMigrationTypeListener implements MigrationTypeListener {
	
	@Autowired
	private NotificationEmailDAO notificationEmailDao;
	
	@Autowired
	private DBOBasicDao basicDao;

	@Override
	public <D extends DatabaseObject<?>> void afterCreateOrUpdate(MigrationType type, List<D> delta) {
		if (type!=MigrationType.VERIFICATION_SUBMISSION) return;
		for (D record : delta) {
			DBOVerificationSubmission dbo = (DBOVerificationSubmission) record;
			VerificationSubmission dto = VerificationDAOImpl.deserializeDTO(dbo); // TODO move to a helper class
			if (dto.getNotificationEmail()!=null) {
				// no backfill required
				return;
			}
			
			// if there is just one captured email then it must have been the notification email
			if(dto.getEmails().size()==1) {
				dto.setNotificationEmail(dto.getEmails().get(0));
			}
			
			// most of the remaining can be disambiguated by the current notification email:
			// TODO ensure that notifications addresses migrate first
			String currentNotificationEmail = notificationEmailDao.getNotificationEmailForPrincipal(Long.parseLong(dto.getCreatedBy()));
			if (dto.getEmails().contains(currentNotificationEmail)) {
				dto.setNotificationEmail(currentNotificationEmail);
			} else {
				// For a tiny number we'll just return the first of the list
				dto.setNotificationEmail(dto.getEmails().get(0));
			}
			
			dbo.setSerialized(VerificationDAOImpl.serializeDTO(dto));
			
			basicDao.update(dbo); // TODO are there any considerations around transactions?
			
		}

	}

}
