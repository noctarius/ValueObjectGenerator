package com.github.vogenerator;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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
		TypeManager typeManager = new TypeManager();

		if (args.length < 5) {
			System.out.println("java -jar generator.jar [PATH] [OUTPATH] [EXT] [TEMPLATE] [TYPEDEFINITION]");
			System.exit(1);
		}

		String path = args[0];
		String outPath = args[1];
		String extension = args[2];
		String templateName = args[3];
		String typeDefName = args[4];

		InputStream types = new FileInputStream(new File(typeDefName));
		LineNumberReader reader = new LineNumberReader(new InputStreamReader(types));
		String line = null;
		while ((line = reader.readLine()) != null) {
			String[] values = line.split("=");
			typeManager.registerType(values[0], values[1], false);
		}

		Parser parser = Parboiled.createParser(Parser.class);
		List<PackageEntity> packageEntities = recursiveWalkPath(new File(path), parser, typeManager);
		if (packageEntities == null) {
			return;
		}
		writeEntities(packageEntities, templateName, outPath, extension, typeManager);
	}

	private static void writeEntities(List<PackageEntity> packageEntities, String templateName, String outPath,
			String extension, TypeManager typeManager) throws IOException {

		InputStream stream = Generator.class.getClassLoader().getResourceAsStream("velocity.properties");
		Properties properties = new Properties();
		properties.load(stream);
		VelocityEngine engine = new VelocityEngine();
		File templateFile = new File(templateName);
		properties.put("file.resource.loader.path", templateFile.getParentFile().getAbsolutePath());
		engine.init(properties);
		Template template = engine.getTemplate(templateFile.getName(), "UTF-8");

		File targetFolder = new File(outPath);
		targetFolder.mkdirs();

		for (PackageEntity packageEntity : packageEntities) {
			System.out.println("Writing package: " + packageEntity.getIdentifier());
			File packageFolder = new File(targetFolder, packageEntity.getIdentifier().replace(".", "/"));
			packageFolder.mkdirs();

			for (PrototypeEntity entity : packageEntity.getEntities()) {
				System.out.println("Writing class: " + entity.getPackageName() + "." + entity.getIdentifier());
				VelocityContext context = new VelocityContext();
				context.put("entity", entity);
				context.put("typeManager", typeManager);
				context.put("support", new Support());

				File outputFile = new File(packageFolder, entity.getIdentifier() + "." + extension);
				FileOutputStream out = new FileOutputStream(outputFile);
				OutputStreamWriter writer = new OutputStreamWriter(out, Charset.forName("UTF-8"));

				template.merge(context, writer);

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
