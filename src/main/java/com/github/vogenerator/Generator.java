package com.github.vogenerator;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.parboiled.Parboiled;
import org.parboiled.Rule;
import org.parboiled.errors.ErrorUtils;
import org.parboiled.parserunners.ReportingParseRunner;
import org.parboiled.support.ParsingResult;

import com.github.vogenerator.objects.PackageEntity;
import com.github.vogenerator.objects.PrototypeEntity;

public class Generator {

	public static void main(String[] args) throws IOException {
		if (args.length < 4) {
			System.out.println("java -jar generator.jar [DSLPATH] [BASEOUTPATH] [OUTPATH] [DEFINITIONFOLDER]");
			System.exit(1);
		}

		File dslPath = new File(args[0]);
		File baseOutPath = new File(args[1]);
		File typeOutPath = new File(args[2]);
		File definitionFolder = new File(args[3]);

		if (!definitionFolder.exists()) {
			System.out.println("Definition folder does not exists");
			System.exit(2);
		}

		if (!dslPath.exists()) {
			System.out.println("Folder / File with DSL definitions does not exists");
			System.exit(3);
		}

		new Generator(dslPath, baseOutPath, typeOutPath, definitionFolder);
	}

	public Generator(File dslPath, File baseOutPath, File typeOutPath, File definitionFolder) throws IOException {
		TypeManager typeManager = new TypeManager();

		File generatorProperties = new File(definitionFolder, "generator.properties");

		Properties generator = new Properties();
		generator.load(new FileReader(generatorProperties));

		String baseTemplateName = generator.getProperty("basetemplate");
		String typeTemplateName = generator.getProperty("typetemplate");
		String typeDefName = generator.getProperty("types");
		String extension = generator.getProperty("extension");

		InputStream types = new FileInputStream(new File(definitionFolder, typeDefName));
		LineNumberReader reader = new LineNumberReader(new InputStreamReader(types));
		String line = null;
		while ((line = reader.readLine()) != null) {
			String[] values = line.split("=");
			typeManager.registerType(values[0], values[1], false);
		}

		Parser parser = Parboiled.createParser(Parser.class);
		List<PackageEntity> packageEntities = recursiveWalkPath(dslPath, parser, typeManager);
		if (packageEntities == null) {
			return;
		}

		writeEntities(packageEntities, definitionFolder, baseTemplateName, typeTemplateName, baseOutPath, typeOutPath,
				extension, typeManager);
	}

	private void writeEntities(List<PackageEntity> packageEntities, File definitionFolder, String baseTemplateName,
			String typeTemplateName, File baseOutPath, File typeOutPath, String extension, TypeManager typeManager)
			throws IOException {

		InputStream stream = Generator.class.getClassLoader().getResourceAsStream("velocity.properties");
		Properties properties = new Properties();
		properties.load(stream);
		VelocityEngine engine = new VelocityEngine();
		properties.put("file.resource.loader.path", definitionFolder.getAbsolutePath());
		engine.init(properties);

		File baseTemplateFile = new File(baseTemplateName);
		Template baseTemplate = engine.getTemplate(baseTemplateFile.getName(), "UTF-8");

		File typeTemplateFile = new File(typeTemplateName);
		Template typeTemplate = engine.getTemplate(typeTemplateFile.getName(), "UTF-8");

		baseOutPath.mkdirs();
		typeOutPath.mkdirs();

		for (PackageEntity packageEntity : packageEntities) {
			System.out.println("Writing package: " + packageEntity.getIdentifier());

			File basePackageFolder = new File(baseOutPath, packageEntity.getIdentifier().replace(".", "/"));
			basePackageFolder.mkdirs();

			File typePackageFolder = new File(typeOutPath, packageEntity.getIdentifier().replace(".", "/"));
			typePackageFolder.mkdirs();

			for (PrototypeEntity entity : packageEntity.getEntities()) {
				VelocityContext context = new VelocityContext();
				context.put("entity", entity);
				context.put("typeManager", typeManager);
				context.put("support", new Support());

				System.out.println("Writing baseclass: " + entity.getPackageName() + ".Base" + entity.getIdentifier());
				File outputFile = new File(basePackageFolder, entity.getIdentifier() + "." + extension);
				FileOutputStream out = new FileOutputStream(outputFile);
				OutputStreamWriter writer = new OutputStreamWriter(out, Charset.forName("UTF-8"));

				baseTemplate.merge(context, writer);

				writer.flush();
				writer.close();

				System.out.println("Writing class: " + entity.getPackageName() + "." + entity.getIdentifier());
				outputFile = new File(typePackageFolder, entity.getIdentifier() + "." + extension);
				out = new FileOutputStream(outputFile);
				writer = new OutputStreamWriter(out, Charset.forName("UTF-8"));

				typeTemplate.merge(context, writer);

				writer.flush();
				writer.close();
			}
		}
	}

	private static List<PackageEntity> recursiveWalkPath(File path, Parser parser, TypeManager typeManager)
			throws IOException {

		List<PackageEntity> packageEntities = new ArrayList<PackageEntity>();
		if (path.isDirectory()) {
			for (File child : path.listFiles()) {
				List<PackageEntity> result = recursiveWalkPath(child, parser, typeManager);
				if (result == null) {
					return null;
				}
				packageEntities.addAll(result);
			}
		}

		if (path.isFile() && path.getName().toUpperCase().endsWith(".VO")) {
			PackageEntity packageEntity = analyseSourcefile(path, parser, typeManager);
			if (packageEntity == null) {
				return null;
			}
			packageEntities.add(packageEntity);
		}

		return packageEntities;
	}

	@SuppressWarnings("rawtypes")
	private static PackageEntity analyseSourcefile(File file, Parser parser, TypeManager typeManager)
			throws IOException {
		String source = readFile(new FileInputStream(file));
		Rule root = parser.CompilationUnit();
		ParsingResult<?> ast = new ReportingParseRunner(root).run(source);

		if (!ast.matched) {
			System.err.println(String.format("\nParse error(s) in File: %s\n%s", file.getAbsolutePath(),
					ErrorUtils.printParseErrors(ast)));
			return null;
		} else {
			// System.out.println(ParseTreeUtils.printNodeTree(ast));
			PackageEntity packageEntity = new Analyser(ast).analyse();
			// System.out.println(packageEntity);

			for (PrototypeEntity entity : packageEntity.getEntities()) {
				String canonicalName = entity.getPackageName() + "." + entity.getIdentifier();
				typeManager.registerType(canonicalName, canonicalName, entity.isEnumType());
				typeManager.registerType(entity.getIdentifier(), canonicalName, entity.isEnumType());
			}

			return packageEntity;
		}
	}

	private static String readFile(InputStream stream) throws IOException {
		BufferedInputStream bis = new BufferedInputStream(stream);
		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		int result = bis.read();
		while (result != -1) {
			byte b = (byte) result;
			buf.write(b);
			result = bis.read();
		}
		return buf.toString();
	}

}
