/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
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

package com.liferay.source.formatter.checks;

import com.liferay.petra.string.CharPool;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.util.MapUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.source.formatter.parser.JavaClass;
import com.liferay.source.formatter.parser.JavaTerm;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Hugo Huijser
 */
public class JavaCleanUpMethodVariablesCheck extends BaseJavaTermCheck {

	@Override
	public boolean isPortalCheck() {
		return true;
	}

	@Override
	protected String doProcess(
		String fileName, String absolutePath, JavaTerm javaTerm,
		String fileContent) {

		JavaClass javaClass = (JavaClass)javaTerm;

		String className = javaClass.getName();

		if (!className.endsWith("Tag")) {
			return javaTerm.getContent();
		}

		String cleanUpMethodContent = _getCleanUpMethodContent(javaClass);

		if (cleanUpMethodContent != null) {
			String newCleanUpMethodContent = _formatVariables(
				fileName, cleanUpMethodContent, javaClass);

			if (!cleanUpMethodContent.equals(newCleanUpMethodContent)) {
				return StringUtil.replace(
					javaTerm.getContent(), cleanUpMethodContent,
					newCleanUpMethodContent);
			}
		}

		return javaTerm.getContent();
	}

	@Override
	protected String[] getCheckableJavaTermNames() {
		return new String[] {JAVA_CLASS};
	}

	private void _checkMissingVariable(
		String fileName, String variableName, JavaClass javaClass) {

		String setterMethodName = "set" + variableName.substring(1);

		for (JavaTerm javaTerm : javaClass.getChildJavaTerms()) {
			if (!javaTerm.isJavaMethod()) {
				continue;
			}

			if (StringUtil.equalsIgnoreCase(
					javaTerm.getName(), setterMethodName)) {

				addMessage(
					fileName,
					"Variable '" + variableName +
						"' is missing in method 'cleanUp'");

				return;
			}
		}
	}

	private String _formatVariables(
		String fileName, String cleanUpMethodContent, JavaClass javaClass) {

		int previousPos = -1;

		for (JavaTerm javaTerm : javaClass.getChildJavaTerms()) {
			if (!javaTerm.isJavaVariable()) {
				continue;
			}

			String accessModifier = javaTerm.getAccessModifier();

			if (!accessModifier.equals(JavaTerm.ACCESS_MODIFIER_PRIVATE)) {
				continue;
			}

			String variableName = javaTerm.getName();

			int pos = cleanUpMethodContent.indexOf(variableName + " =");

			if (pos == -1) {
				_checkMissingVariable(fileName, variableName, javaClass);

				continue;
			}

			if (previousPos > pos) {
				return _sortVariables(cleanUpMethodContent, previousPos, pos);
			}

			previousPos = pos;

			Pattern pattern = Pattern.compile(
				"\t(private|protected|public)\\s+" +
					"(((final|static|transient|volatile)( |\n))*)([\\s\\S]*?)" +
						variableName);

			String variableContent = javaTerm.getContent();

			Matcher matcher = pattern.matcher(variableContent);

			if (!matcher.find()) {
				continue;
			}

			String modifierDefinition = StringUtil.trim(
				variableContent.substring(matcher.start(1), matcher.start(6)));

			if (modifierDefinition.contains("final")) {
				continue;
			}

			String javaFieldType = StringUtil.trim(matcher.group(6));

			pattern = Pattern.compile(
				javaTerm.getName() + " =\\s+[a-z]\\w*\\.");

			matcher = pattern.matcher(cleanUpMethodContent);

			if (matcher.find()) {
				continue;
			}

			int x = variableContent.indexOf(javaTerm.getName());

			String defaultValue = _getDefaultValue(javaFieldType);

			String setVariableCommand = variableContent.substring(x);

			if (!setVariableCommand.contains(" =")) {
				setVariableCommand = StringUtil.replaceLast(
					setVariableCommand, CharPool.SEMICOLON,
					" = " + defaultValue + ";");
			}

			setVariableCommand = StringUtil.replace(
				setVariableCommand,
				new String[] {StringPool.TAB, StringPool.NEW_LINE},
				new String[] {StringPool.BLANK, StringPool.SPACE});

			String setVariableCommandRegex = StringUtil.replace(
				setVariableCommand,
				new String[] {
					StringPool.CLOSE_PARENTHESIS, StringPool.OPEN_PARENTHESIS,
					StringPool.SPACE, "0\\.0"
				},
				new String[] {"\\)", "\\(", "\\s*", "0(\\.0)?"});

			pattern = Pattern.compile(setVariableCommandRegex);

			matcher = pattern.matcher(cleanUpMethodContent);

			if (!matcher.find()) {
				addMessage(
					fileName,
					"Initial value for '" + variableName +
						"' differs from value in cleanUp method",
					"cleanup.markdown");
			}
		}

		return cleanUpMethodContent;
	}

	private String _getCleanUpMethodContent(JavaClass javaClass) {
		for (JavaTerm javaTerm : javaClass.getChildJavaTerms()) {
			if (!javaTerm.isJavaMethod()) {
				continue;
			}

			String javaTermName = javaTerm.getName();

			if (javaTermName.equals("cleanUp")) {
				return javaTerm.getContent();
			}
		}

		return null;
	}

	private String _getDefaultValue(String type) {
		if (StringUtil.isLowerCase(type)) {
			return _defaultPrimitiveValues.get(type);
		}

		return "null";
	}

	private String _sortVariables(
		String cleanUpMethodContent, int previousPos, int pos) {

		int semiColonPos = cleanUpMethodContent.indexOf(";\n", pos);

		if ((semiColonPos == -1) || (semiColonPos > previousPos)) {
			return cleanUpMethodContent;
		}

		int previousSemiColonPos = cleanUpMethodContent.indexOf(
			";\n", previousPos);

		if (previousSemiColonPos == -1) {
			return cleanUpMethodContent;
		}

		String previousVariableSetter = cleanUpMethodContent.substring(
			previousPos, previousSemiColonPos + 1);
		String variableSetter = cleanUpMethodContent.substring(
			pos, semiColonPos + 1);

		String newCleanUpMethodContent = StringUtil.replaceFirst(
			cleanUpMethodContent, variableSetter, previousVariableSetter);

		return StringUtil.replaceLast(
			newCleanUpMethodContent, previousVariableSetter, variableSetter);
	}

	private static final Map<String, String> _defaultPrimitiveValues =
		MapUtil.fromArray(
			new String[] {
				"boolean", "false", "char", "'\\\\0'", "byte", "0", "double",
				"0\\.0", "float", "0\\.0", "int", "0", "long", "0", "short", "0"
			});

}