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

package com.liferay.css.builder;

import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;

import java.io.File;
import java.io.IOException;

import java.net.URL;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Eduardo Garcia
 * @author David Truong
 */
public class CSSBuilderTest {

	@BeforeClass
	public static void setUpClass() throws Exception {
		URL url = CSSBuilderTest.class.getResource("dependencies");

		Path path = Paths.get(url.toURI());

		_docrootDirName = path.toString();
	}

	@After
	public void tearDown() throws Exception {
		Files.walkFileTree(
			Paths.get(_docrootDirName + "/css/.sass-cache"),
			new SimpleFileVisitor<Path>() {

				@Override
				public FileVisitResult postVisitDirectory(
						Path path, IOException ioe)
					throws IOException {

					Files.delete(path);

					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFile(
						Path path, BasicFileAttributes basicFileAttributes)
					throws IOException {

					Files.delete(path);

					return FileVisitResult.CONTINUE;
				}

			});
	}

	@Test
	public void testCssBuilderWithFragmentChange() throws Exception {
		Path fragmentFileToChange = Paths.get(
			_docrootDirName, "css", "_import_change.scss");

		_changeContentInPath(fragmentFileToChange, "brown", "khaki");

		executeCSSBuilder(
			"/css", _docrootDirName, false, ".sass-cache/",
			_PORTAL_COMMON_CSS_DIR_NAME, 6, new String[0], "jni");

		String outputCssFilePath =
			_docrootDirName + "/css/.sass-cache/test_import_change.css";

		String outputCssFileContent = _read(outputCssFilePath);

		_changeContentInPath(fragmentFileToChange, "khaki", "brown");

		executeCSSBuilder(
			"/css", _docrootDirName, false, ".sass-cache/",
			_PORTAL_COMMON_CSS_DIR_NAME, 6, new String[0], "jni");

		outputCssFileContent = _read(outputCssFilePath);

		Assert.assertTrue(outputCssFileContent.contains("brown"));
	}

	@Test
	public void testCssBuilderWithJni() throws Exception {
		_testCssBuilder("jni", _PORTAL_COMMON_CSS_DIR_NAME);
	}

	@Test
	public void testCssBuilderWithJniAndPortalCommonJar() throws Exception {
		_testCssBuilder("jni", _PORTAL_COMMON_CSS_JAR_FILE_NAME);
	}

	@Test
	public void testCssBuilderWithRuby() throws Exception {
		_testCssBuilder("ruby", _PORTAL_COMMON_CSS_DIR_NAME);
	}

	@Test
	public void testCssBuilderWithRubyAndPortalCommonJar() throws Exception {
		_testCssBuilder("ruby", _PORTAL_COMMON_CSS_DIR_NAME);
	}

	protected void executeCSSBuilder(
			String dirName, String docrootDirName, boolean generateSourceMap,
			String outputDirName, String portalCommonPath, int precision,
			String[] rtlExcludedPathRegexps, String sassCompilerClassName)
		throws Exception {

		try (CSSBuilder cssBuilder = new CSSBuilder(
				docrootDirName, generateSourceMap, outputDirName,
				portalCommonPath, precision, rtlExcludedPathRegexps,
				sassCompilerClassName)) {

			cssBuilder.execute(Collections.singletonList(dirName));
		}
	}

	private static void _changeContentInPath(
			Path path, String s, String replacement)
		throws Exception {

		Charset charset = StandardCharsets.UTF_8;

		String content = new String(Files.readAllBytes(path), charset);

		content = content.replace(s, replacement);

		Files.write(path, content.getBytes(charset));
	}

	private void _assertMatchesCount(
		Pattern pattern, String s, int expectedCount) {

		int count = 0;

		Matcher matcher = pattern.matcher(s);

		while (matcher.find()) {
			count++;
		}

		Assert.assertEquals(expectedCount, count);
	}

	private String _read(String fileName) throws Exception {
		Path path = Paths.get(fileName);

		String s = new String(Files.readAllBytes(path), StringPool.UTF8);

		return StringUtil.replace(
			s, StringPool.RETURN_NEW_LINE, StringPool.NEW_LINE);
	}

	private void _testCssBuilder(String compiler, String portalCommonCssPath)
		throws Exception {

		executeCSSBuilder(
			"/css", _docrootDirName, false, ".sass-cache/", portalCommonCssPath,
			6, new String[0], compiler);

		String expectedTestContent = _read(
			_docrootDirName + "/expected/test.css");

		String actualTestContent = _read(
			_docrootDirName + "/css/.sass-cache/test.css");

		Assert.assertEquals(expectedTestContent, actualTestContent);

		String actualTestCssImportContent = _read(
			_docrootDirName + "/css/.sass-cache/test_css_import.css");

		_assertMatchesCount(_cssImportPattern, actualTestCssImportContent, 3);

		String actualTestCssImportRtlContent = _read(
			_docrootDirName + "/css/.sass-cache/test_css_import_rtl.css");

		_assertMatchesCount(
			_cssImportPattern, actualTestCssImportRtlContent, 3);

		Assert.assertEquals(expectedTestContent, actualTestContent);

		String actualTestPartialContent = _read(
			_docrootDirName + "/css/.sass-cache/test_partial.css");

		Assert.assertEquals(expectedTestContent, actualTestPartialContent);

		File partialCssFile = new File(
			Paths.get("/css/.sass-cache/_partial.css").toString());

		Assert.assertFalse(partialCssFile.exists());

		String expectedTestRtlContent = _read(
			_docrootDirName + "/expected/test_rtl.css");

		String actualTestRtlContent = _read(
			_docrootDirName + "/css/.sass-cache/test_rtl.css");

		Assert.assertEquals(expectedTestRtlContent, actualTestRtlContent);

		String actualTestPartialRtlContent = _read(
			_docrootDirName + "/css/.sass-cache/test_partial_rtl.css");

		Assert.assertEquals(
			expectedTestRtlContent, actualTestPartialRtlContent);

		String expectedUnicodeContent = _read(
			_docrootDirName + "/expected/test_unicode.css");

		String actualTestUnicodeContent = _read(
			_docrootDirName + "/css/.sass-cache/test_unicode.css");

		Assert.assertEquals(expectedUnicodeContent, actualTestUnicodeContent);
	}

	private static final String _PORTAL_COMMON_CSS_DIR_NAME =
		"build/portal-common-css";

	private static final String _PORTAL_COMMON_CSS_JAR_FILE_NAME =
		"build/portal-common-css-jar/com.liferay.frontend.css.common.jar";

	private static final Pattern _cssImportPattern = Pattern.compile(
		"@import\\s+url\\s*\\(\\s*['\"]?(.+\\.css\\?t=\\d+)['\"]?\\s*\\)\\s*;");
	private static String _docrootDirName;

}