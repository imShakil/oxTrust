/*
 * oxTrust is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxtrust.ldap.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang.NotImplementedException;
import org.gluu.persist.PersistenceEntryManager;
import org.gluu.persist.ldap.impl.LdifDataUtility;
import org.gluu.persist.ldap.operation.LdapOperationService;
import org.gluu.persist.operation.PersistenceOperationService;
import org.slf4j.Logger;

import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldif.LDIFReader;

/**
 * Provides operations with LDIF files
 * 
 * @author Shekhar L Date: 02.28.2017
 * @author Yuriy Movchan Date: 03/06/2017
 */
@Stateless
@Named("ldifService")
public class LdifService implements Serializable {

	private static final String CLOSE = "]";
	private static final String OPEN = "[";
	private static final String LINE_SEPARATOR = "line.separator";

	private static final long serialVersionUID = 6690460114767359078L;

	@Inject
	private Logger log;

	@Inject
	private PersistenceEntryManager ldapEntryManager;

	public ResultCode importLdifFileInLdap(InputStream is) throws LDAPException {
		ResultCode result = ResultCode.UNAVAILABLE;
		PersistenceOperationService persistenceOperationService = ldapEntryManager.getOperationService();
		if (!(persistenceOperationService instanceof LdapOperationService)) {
			throw new NotImplementedException("Current Persistence mechanism not allows to import data from LDIF!");
		}
		LdapOperationService ldapOperationService = (LdapOperationService) persistenceOperationService;
		LDAPConnection connection = ldapOperationService.getConnection();
		try {
			LdifDataUtility ldifDataUtility = LdifDataUtility.instance();
			LDIFReader importLdifReader = new LDIFReader(is);

			result = ldifDataUtility.importLdifFile(connection, importLdifReader);
			importLdifReader.close();
		} catch (Exception ex) {
			log.error("Failed to import ldif file: ", ex);
		} finally {
			ldapOperationService.releaseConnection(connection);
		}
		return result;
	}

	public ResultCode validateLdifFile(InputStream is, String dn) throws LDAPException {
		ResultCode result = ResultCode.UNAVAILABLE;
		try {
			LdifDataUtility ldifDataUtility = LdifDataUtility.instance();
			LDIFReader validateLdifReader = new LDIFReader(is);
			result = ldifDataUtility.validateLDIF(validateLdifReader, dn);
			validateLdifReader.close();
		} catch (Exception ex) {
			log.error("Failed to validate ldif file: ", ex);
		}
		return result;
	}

	public void exportLDIFFile(List<String> checkedItems, OutputStream output) throws LDAPException {
		try {
			StringBuilder builder = new StringBuilder();
			if (checkedItems != null && checkedItems.size() > 0) {
				checkedItems.stream().forEach(e -> {
					String[] exportEntry = ldapEntryManager.exportEntry(e);
					if (exportEntry != null && exportEntry.length >= 0) {
						Stream.of(exportEntry).forEach(v -> {
							if (v.split(":")[1].trim().startsWith(OPEN) && v.split(":")[1].trim().endsWith(CLOSE)) {
								String key = v.split(":")[0];
								String values = v.split(":")[1];
								values = values.replaceFirst(OPEN, "");
								values = replaceLast(values, CLOSE, "");
								List<String> list = Pattern.compile(", ").splitAsStream(values.trim())
										.collect(Collectors.toList());
								for (String value : list) {
									builder.append(key + ": " + value);
								}
							} else {
								builder.append(v);
							}
							builder.append(System.getProperty(LINE_SEPARATOR));
						});
					}
					builder.append(System.getProperty(LINE_SEPARATOR));
				});
			}
			output.write(builder.toString().getBytes());
		} catch (IOException e) {
			log.error("Error while exporting entries: ", e);
		}
	}
	
	String replaceLast(String mainString, String substring, String replacement)
	{
	  int index = mainString.lastIndexOf(substring);
	  if (index == -1)
	    return mainString;
	  return mainString.substring(0, index) + replacement
	          + mainString.substring(index+substring.length()).trim();
	}

}
