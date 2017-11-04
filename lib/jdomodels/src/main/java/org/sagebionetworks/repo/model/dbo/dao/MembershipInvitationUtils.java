package org.sagebionetworks.repo.model.dbo.dao;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.MembershipInvitation;
import org.sagebionetworks.repo.model.dbo.persistence.DBOMembershipInvtnSubmission;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;

import com.amazonaws.util.IOUtils;

public class MembershipInvitationUtils {

	// the convention is that the individual fields take precedence
	// over the serialized objects.  When restoring the dto we first deserialize
	// the 'blob' and then populate the individual fields

	public static final String CLASS_ALIAS = "MembershipInvitation";

	public static void copyDtoToDbo(MembershipInvitation dto, DBOMembershipInvtnSubmission dbo) throws DatastoreException {
		if (dto.getId()!=null) dbo.setId(Long.parseLong(dto.getId()));
		dbo.setCreatedOn(dto.getCreatedOn().getTime());
		if(dto.getExpiresOn()==null) dbo.setExpiresOn(null); else dbo.setExpiresOn(dto.getExpiresOn().getTime());
		dbo.setTeamId(Long.parseLong(dto.getTeamId()));
		if (dto.getInviteeId()==null) dbo.setInviteeId(null); else dbo.setInviteeId(Long.parseLong(dto.getInviteeId()));
		if (dto.getInviteeEmail()==null) dbo.setInviteeEmail(null); else dbo.setInviteeEmail(dto.getInviteeEmail());
		copyToSerializedField(dto, dbo);
	}

	public static MembershipInvitation copyDboToDto(DBOMembershipInvtnSubmission dbo) throws DatastoreException {
		MembershipInvitation dto = copyFromSerializedField(dbo);
		dto.setId(dbo.getId().toString());
		dto.setCreatedOn(new Date(dbo.getCreatedOn()));
		if (dbo.getExpiresOn()==null) dto.setExpiresOn(null); else dto.setExpiresOn(new Date(dbo.getExpiresOn()));
		dto.setTeamId(dbo.getTeamId().toString());
		if (dbo.getInviteeId()==null) dto.setInviteeId(null); else dto.setInviteeId(dbo.getInviteeId().toString());
		if (dbo.getInviteeEmail()==null) dto.setInviteeEmail(null); else dto.setInviteeEmail(dbo.getInviteeEmail());
		return dto;
	}

	public static void copyToSerializedField(MembershipInvitation dto, DBOMembershipInvtnSubmission dbo) throws DatastoreException {
		try {
			dbo.setProperties(JDOSecondaryPropertyUtils.compressObject(dto, CLASS_ALIAS));
		} catch (IOException e) {
			throw new DatastoreException(e);
		}
	}
	
	public static MembershipInvitation deserialize(byte[] b) {
		try {
			return (MembershipInvitation)JDOSecondaryPropertyUtils.decompressedObject(b, CLASS_ALIAS, MembershipInvitation.class);
		} catch (IOException e) {
			throw new DatastoreException(e);
		}
		
	}
	
	public static MembershipInvitation copyFromSerializedField(DBOMembershipInvtnSubmission dbo) throws DatastoreException {
		return deserialize(dbo.getProperties());
	}

	public static byte[] unzip(byte[] zippedBytes) throws IOException {
		ByteArrayInputStream inputStream = new ByteArrayInputStream(zippedBytes);
		GZIPInputStream gzipInputStream = new GZIPInputStream(inputStream);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		IOUtils.copy(gzipInputStream, outputStream);
		return outputStream.toByteArray();
	}

	public static byte[] zip(byte[] unzippedBytes) throws IOException {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		GZIPOutputStream gzip = new GZIPOutputStream(outputStream);
		gzip.write(unzippedBytes);
		gzip.finish();
		return outputStream.toByteArray();
	}
}
