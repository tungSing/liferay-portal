/**
 * Copyright (c) 2000-2010 Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.portal.verify;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.search.SearchEngineUtil;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.NotificationThreadLocal;
import com.liferay.portal.kernel.util.PropsKeys;
import com.liferay.portal.kernel.workflow.WorkflowThreadLocal;
import com.liferay.portal.service.persistence.BatchSessionUtil;
import com.liferay.portal.util.PropsUtil;
import com.liferay.portal.util.PropsValues;

/**
 * <a href="VerifyProcessUtil.java.html"><b><i>View Source</i></b></a>
 *
 * @author Brian Wing Shun Chan
 * @author Alexander Chow
 * @author Raymond Augé
 */
public class VerifyProcessUtil {

	public static boolean verifyProcess(
			boolean ranUpgradeProcess, boolean verified)
		throws VerifyException {

		boolean ranVerifyProcess = false;

		int verifyFrequency = GetterUtil.getInteger(
			PropsUtil.get(PropsKeys.VERIFY_FREQUENCY));

		if ((verifyFrequency == VerifyProcess.ALWAYS) ||
			((verifyFrequency == VerifyProcess.ONCE) && !verified) ||
			(ranUpgradeProcess)) {

			if (ranUpgradeProcess && PropsValues.INDEX_ON_UPGRADE) {
				PropsUtil.set(
					PropsKeys.INDEX_ON_STARTUP, Boolean.TRUE.toString());
			}

			String[] verifyProcessClassNames = PropsUtil.getArray(
				PropsKeys.VERIFY_PROCESSES);

			BatchSessionUtil.setEnabled(true);
			NotificationThreadLocal.setEnabled(false);
			WorkflowThreadLocal.setEnabled(false);

			boolean tempIndexReadOnly = SearchEngineUtil.isIndexReadOnly();

			SearchEngineUtil.setIndexReadOnly(true);

			try {
				for (String verifyProcessClassName : verifyProcessClassNames) {
					boolean tempRanVerifyProcess = _verifyProcess(
						verifyProcessClassName);

					if (tempRanVerifyProcess) {
						ranVerifyProcess = true;
					}
				}
			}
			finally {
				BatchSessionUtil.setEnabled(false);
				NotificationThreadLocal.setEnabled(true);
				WorkflowThreadLocal.setEnabled(true);

				SearchEngineUtil.setIndexReadOnly(tempIndexReadOnly);
			}
		}

		return ranVerifyProcess;
	}

	private static boolean _verifyProcess(String verifyProcessClassName)
		throws VerifyException {

		if (_log.isDebugEnabled()) {
			_log.debug("Initializing verification " + verifyProcessClassName);
		}

		try {
			VerifyProcess verifyProcess = (VerifyProcess)Class.forName(
				verifyProcessClassName).newInstance();

			if (_log.isDebugEnabled()) {
				_log.debug("Running verification " + verifyProcessClassName);
			}

			verifyProcess.verify();

			if (_log.isDebugEnabled()) {
				_log.debug("Finished verification " + verifyProcessClassName);
			}

			return true;
		}
		catch (ClassNotFoundException cnfe) {
			_log.error(verifyProcessClassName + " cannot be found");
		}
		catch (IllegalAccessException iae) {
			_log.error(verifyProcessClassName + " cannot be accessed");
		}
		catch (InstantiationException ie) {
			_log.error(verifyProcessClassName + " cannot be initiated");
		}

		return false;
	}

	private static Log _log = LogFactoryUtil.getLog(VerifyProcessUtil.class);

}