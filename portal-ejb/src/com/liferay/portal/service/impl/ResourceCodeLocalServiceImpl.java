/**
 * Copyright (c) 2000-2007 Liferay, Inc. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.liferay.portal.service.impl;

import com.liferay.counter.service.CounterLocalServiceUtil;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.kernel.util.StringMaker;
import com.liferay.portal.model.ResourceCode;
import com.liferay.portal.service.base.ResourceCodeLocalServiceBaseImpl;
import com.liferay.portal.service.persistence.ResourceCodeUtil;
import com.liferay.util.CollectionFactory;

import java.util.Map;

/**
 * <a href="ResourceCodeLocalServiceImpl.java.html"><b><i>View Source</i></b>
 * </a>
 *
 * @author Brian Wing Shun Chan
 *
 */
public class ResourceCodeLocalServiceImpl
	extends ResourceCodeLocalServiceBaseImpl {

	public ResourceCode getResourceCode(long codeId)
		throws PortalException, SystemException {

		return ResourceCodeUtil.findByPrimaryKey(codeId);
	}

	public ResourceCode getResourceCode(
			String companyId, String name, String scope)
		throws PortalException, SystemException {

		// Always cache the resource code. This table exists to improve
		// performance. Create the resource code if one does not exist.

		String key = encodeKey(companyId, name, scope);

		ResourceCode resourceCode = (ResourceCode)_resourceCodes.get(key);

		if (resourceCode == null) {
			resourceCode = ResourceCodeUtil.fetchByC_N_S(
				companyId, name, scope);

			if (resourceCode == null) {
				long codeId = CounterLocalServiceUtil.increment(
					ResourceCode.class.getName());

				resourceCode = ResourceCodeUtil.create(codeId);

				resourceCode.setCompanyId(companyId);
				resourceCode.setName(name);
				resourceCode.setScope(scope);

				ResourceCodeUtil.update(resourceCode);
			}

			_resourceCodes.put(key, resourceCode);
		}

		return resourceCode;
	}

	protected String encodeKey(String companyId, String name, String scope) {
		StringMaker sm = new StringMaker();

		sm.append(companyId);
		sm.append(name);
		sm.append(scope);

		return sm.toString();
	}

	private static Map _resourceCodes = CollectionFactory.getSyncHashMap();

}