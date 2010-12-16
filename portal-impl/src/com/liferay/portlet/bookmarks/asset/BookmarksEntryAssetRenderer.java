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

package com.liferay.portlet.bookmarks.asset;

import com.liferay.portal.kernel.portlet.LiferayPortletRequest;
import com.liferay.portal.kernel.portlet.LiferayPortletResponse;
import com.liferay.portal.kernel.util.HtmlUtil;
import com.liferay.portal.security.permission.ActionKeys;
import com.liferay.portal.security.permission.PermissionChecker;
import com.liferay.portal.theme.ThemeDisplay;
import com.liferay.portal.util.PortletKeys;
import com.liferay.portal.util.WebKeys;
import com.liferay.portlet.asset.model.BaseAssetRenderer;
import com.liferay.portlet.bookmarks.model.BookmarksEntry;
import com.liferay.portlet.bookmarks.service.permission.BookmarksEntryPermission;

import javax.portlet.PortletURL;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

/**
 * @author Julio Camarero
 * @author Juan Fernández
 */
public class BookmarksEntryAssetRenderer extends BaseAssetRenderer {

	public BookmarksEntryAssetRenderer(BookmarksEntry entry) {
		_entry = entry;
	}

	public long getClassPK() {
		return _entry.getEntryId();
	}

	public long getGroupId() {
		return _entry.getGroupId();
	}

	public String getSummary() {
		return HtmlUtil.stripHtml(_entry.getComments());
	}

	public String getTitle() {
		return _entry.getName();
	}

	public PortletURL getURLEdit(
		LiferayPortletRequest liferayPortletRequest,
		LiferayPortletResponse liferayPortletResponse) {

		PortletURL portletURL = liferayPortletResponse.createRenderURL(
			PortletKeys.BOOKMARKS);

		portletURL.setParameter("struts_action", "/bookmarks/edit_entry");
		portletURL.setParameter(
			"folderId", String.valueOf(_entry.getFolderId()));
 		portletURL.setParameter("entryId", String.valueOf(_entry.getEntryId()));

		return portletURL;
	}

	public String getURLViewInContext(
		LiferayPortletRequest liferayPortletRequest,
		LiferayPortletResponse liferayPortletResponse,
		String noSuchEntryRedirect) {

		ThemeDisplay themeDisplay =
			(ThemeDisplay)liferayPortletRequest.getAttribute(
				WebKeys.THEME_DISPLAY);

		return themeDisplay.getPathMain() + "/bookmarks/open_entry?entryId=" +
			_entry.getEntryId();
	}

	public long getUserId() {
		return _entry.getUserId();
	}

	public String getUuid() {
		return _entry.getUuid();
	}

	public boolean hasEditPermission(PermissionChecker permissionChecker) {
		try {
			return BookmarksEntryPermission.contains(
				permissionChecker, _entry, ActionKeys.UPDATE);
		}
		catch (Exception e) {
		}

		return false;
	}

	public boolean hasViewPermission(PermissionChecker permissionChecker) {
		try {
			return BookmarksEntryPermission.contains(
				permissionChecker, _entry, ActionKeys.VIEW);
		}
		catch (Exception e) {
		}

		return true;
	}

	public boolean isPrintable() {
		return true;
	}

	public String render(
			RenderRequest renderRequest, RenderResponse renderResponse,
			String template)
		throws Exception {

		if (template.equals(TEMPLATE_FULL_CONTENT)) {
			renderRequest.setAttribute(WebKeys.BOOKMARKS_ENTRY, _entry);

			return "/html/portlet/bookmarks/asset/" + template + ".jsp";
		}
		else {
			return null;
		}
	}

	protected String getIconPath(ThemeDisplay themeDisplay) {
		return themeDisplay.getPathThemeImages() + "/ratings/star_hover.png";
	}

	private BookmarksEntry _entry;

}