package com.github.vogenerator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.parboiled.Node;
import org.parboiled.common.Predicate;
import org.parboiled.support.ParseTreeUtils;
import org.parboiled.support.ParsingResult;
import org.parboiled.trees.GraphNode;

import com.github.vogenerator.objects.AssignmentEntity;
import com.github.vogenerator.objects.ClassEntity;
import com.github.vogenerator.objects.CommentEntity;
import com.github.vogenerator.objects.EnumConstantEntity;
import com.github.vogenerator.objects.EnumEntity;
import com.github.vogenerator.objects.PackageEntity;
import com.github.vogenerator.objects.PropertyEntity;
import com.github.vogenerator.objects.TransmissionType;

public class Analyser {

	private final ParsingResult<?> ast;

	public Analyser(ParsingResult<?> ast) {
		this.ast = ast;
	}

	public PackageEntity analyse() {
		return analyseCompilationUnit(ast.parseTreeRoot);
	}

	private <T extends GraphNode<T>> PackageEntity analyseCompilationUnit(T graphNode) {
		if (graphNode instanceof Node) {
			Node<?> node = (Node<?>) graphNode;
			Node<?> packageDeclarationNode = ParseTreeUtils.findNodeByLabel(node, "PackageDeclaration");
			if (packageDeclarationNode != null) {
				return analysePackageDeclaration(packageDeclarationNode);
			}
		}

		throw new RuntimeException("Failure during analysing step: No package definition found");
	}

	private <V> PackageEntity analysePackageDeclaration(Node<V> node) {
		PackageEntity packageEntity = new PackageEntity();

		Node<?> identifierNode = ParseTreeUtils.findNodeByLabel(node, "QualifiedIdentifier");
		String identifier = ParseTreeUtils.getNodeText(identifierNode, ast.inputBuffer).trim();
		packageEntity.setIdentifier(identifier);

		List<Node<V>> classDeclarations = new ArrayList<Node<V>>();
		ParseTreeUtils.collectNodes(node.getParent(), new Predicate<Node<V>>() {

			public boolean apply(Node<V> input) {
				return "ClassDeclaration".equals(input.getLabel());
			}
		}, classDeclarations);

		for (Node<V> classDeclaration : classDeclarations) {
			packageEntity.getEntities().add(analyseClassDefinition(classDeclaration, identifier));
		}

		List<Node<V>> componentDeclarations = new ArrayList<Node<V>>();
		ParseTreeUtils.collectNodes(node.getParent(), new Predicate<Node<V>>() {

			public boolean apply(Node<V> input) {
				return "ComponentDeclaration".equals(input.getLabel());
			}
		}, componentDeclarations);

		for (Node<V> componentDeclaration : componentDeclarations) {
			ClassEntity componentEntity = analyseClassDefinition(componentDeclaration, identifier);
			componentEntity.setComponentType(true);
			packageEntity.getEntities().add(componentEntity);
		}

		List<Node<V>> enumDeclarations = new ArrayList<Node<V>>();
		ParseTreeUtils.collectNodes(node.getParent(), new Predicate<Node<V>>() {

			public boolean apply(Node<V> input) {
				return "EnumDeclaration".equals(input.getLabel());
			}
		}, enumDeclarations);

		for (Node<V> enumDeclaration : enumDeclarations) {
			packageEntity.getEntities().add(analyseEnumDefinition(enumDeclaration, identifier));
		}

		return packageEntity;
	}

	private <V> ClassEntity analyseClassDefinition(Node<V> node, String packageName) {
		ClassEntity classEntity = new ClassEntity();

		Node<?> identifierNode = ParseTreeUtils.findNodeByLabel(node, "Identifier");
		String identifier = ParseTreeUtils.getNodeText(identifierNode, ast.inputBuffer).trim();
		classEntity.setIdentifier(identifier);
		classEntity.setPackageName(packageName);

		Node<?> secureNode = ParseTreeUtils.findNodeByLabel(node, "Secure");
		classEntity.setSecureType(secureNode != null);

		Node<?> extendsNode = ParseTreeUtils.findNodeByLabel(node, "Extends");
		if (extendsNode != null) {
			Node<?> qualifierNode = ParseTreeUtils.findNodeByLabel(extendsNode.getParent(), "QualifiedIdentifier");
			if (qualifierNode != null) {
				classEntity.setSuperTypeId(ParseTreeUtils.getNodeText(qualifierNode, ast.inputBuffer).trim());
			}
		}

		Node<V> commentNode = ParseTreeUtils.findNodeByLabel(node, "ClassDocumentingComment");
		if (commentNode != null) {
			classEntity.setCommentEntity(analyseCommentNode(commentNode));
		}

		List<Node<V>> propertyDeclarations = new ArrayList<Node<V>>();
		ParseTreeUtils.collectNodes(node, new Predicate<Node<V>>() {

			public boolean apply(Node<V> input) {
				return "MemberDeclaration".equals(input.getLabel());
			}
		}, propertyDeclarations);

		for (Node<V> propertyDeclaration : propertyDeclarations) {
			classEntity.getPropertyEntities().add(analysePropertyDeclaration(propertyDeclaration));
		}

		Collections.sort(classEntity.getPropertyEntities());
		return classEntity;
	}

	private <V> PropertyEntity analysePropertyDeclaration(Node<V> node) {
		PropertyEntity propertyEntity = new PropertyEntity();

		Node<?> clientNode = ParseTreeUtils.findNodeByLabel(node, "Client");
		Node<?> serverNode = ParseTreeUtils.findNodeByLabel(node, "Server");
		Node<?> readOnlyNode = ParseTreeUtils.findNodeByLabel(node, "ReadOnly");

		TransmissionType transmissionType = clientNode != null ? TransmissionType.Client
				: serverNode != null ? TransmissionType.Server : TransmissionType.Both;

		Node<?> identifierNode = ParseTreeUtils.findNodeByLabel(node, "ReferenceType");
		String typeId = ParseTreeUtils.getNodeText(identifierNode, ast.inputBuffer).trim();

		String identifier = null;
		for (Node<?> child : node.getChildren()) {
			if ("Identifier".equals(child.getLabel())) {
				identifier = ParseTreeUtils.getNodeText(child, ast.inputBuffer);
			}
		}

		if (identifier == null) {
			throw new IllegalStateException("No identifier found for property");
		}

		propertyEntity.setTypeId(typeId);
		propertyEntity.setIdentifier(identifier);
		propertyEntity.setReadOnly(readOnlyNode != null);
		propertyEntity.setTransmissionType(transmissionType);

		Node<?> assignmentNode = ParseTreeUtils.findNodeByLabel(node, "DirectAssignment");
		if (assignmentNode != null) {
			Node<?> literal = ParseTreeUtils.findNodeByLabel(assignmentNode, "Instantiation");
			if (literal != null) {
				propertyEntity.setAssignmentEntity(analyseInstantiation(literal, typeId));
			} else {
				literal = ParseTreeUtils.findNodeByLabel(assignmentNode, "Literal");
				if (literal != null) {
					propertyEntity.setAssignmentEntity(analyseLiteral(literal, typeId));
				}
			}
		} else {
			Node<?> memberBindingNode = ParseTreeUtils.findNodeByLabel(node, "MemberBinding");
			if (memberBindingNode != null) {
				Node<?> bindingIdentifierNode = ParseTreeUtils.findNodeByLabel(node, "Identifier");
				String bindingIdentifier = ParseTreeUtils.getNodeText(bindingIdentifierNode, ast.inputBuffer).trim();
				propertyEntity.setBoundProperty(bindingIdentifier);
			}
		}

		Node<V> commentNode = ParseTreeUtils.findNodeByLabel(node, "DocumentingComment");
		if (commentNode != null) {
			propertyEntity.setCommentEntity(analyseCommentNode(commentNode));
		}

		return propertyEntity;
	}

	private <V> AssignmentEntity analyseInstantiation(Node<V> node, String typeId) {
		Node<?> qualifierNode = ParseTreeUtils.findNodeByLabel(node.getParent(), "QualifiedIdentifier");
		if (qualifierNode != null) {
			AssignmentEntity assignmentEntity = new AssignmentEntity();
			assignmentEntity.setTypeId(typeId);
			assignmentEntity.setInstantiation(true);
			assignmentEntity.setValue(ParseTreeUtils.getNodeText(qualifierNode, ast.inputBuffer).trim());
			return assignmentEntity;
		}
		throw new IllegalStateException("No identifier found for property");
	}

	private <V> AssignmentEntity analyseLiteral(Node<V> node, String typeId) {
		Node<?> literalNode = ParseTreeUtils.findNodeByLabel(node.getParent(), "Literal");
		if (literalNode != null) {
			AssignmentEntity assignmentEntity = new AssignmentEntity();
			assignmentEntity.setTypeId(typeId);
			assignmentEntity.setInstantiation(false);
			assignmentEntity.setValue(ParseTreeUtils.getNodeText(literalNode, ast.inputBuffer).trim());
			return assignmentEntity;
		}
		throw new IllegalStateException("No literal found for property");
	}

	private <V> EnumEntity analyseEnumDefinition(Node<V> node, String packageName) {
		EnumEntity enumEntity = new EnumEntity();

		Node<?> identifierNode = ParseTreeUtils.findNodeByLabel(node, "Identifier");
		String identifier = ParseTreeUtils.getNodeText(identifierNode, ast.inputBuffer).trim();
		enumEntity.setIdentifier(identifier);
		enumEntity.setPackageName(packageName);

		List<Node<V>> propertyDeclarations = new ArrayList<Node<V>>();
		ParseTreeUtils.collectNodes(node, new Predicate<Node<V>>() {

			public boolean apply(Node<V> input) {
				return "EnumConstantDeclarations".equals(input.getLabel());
			}
		}, propertyDeclarations);

		for (Node<V> propertyDeclaration : propertyDeclarations) {
			enumEntity.getConstants().addAll(analyseEnumConstantsDeclaration(propertyDeclaration));
		}

		Collections.sort(enumEntity.getConstants());
		return enumEntity;
	}

	private <V> List<EnumConstantEntity> analyseEnumConstantsDeclaration(Node<V> node) {
		List<Node<V>> enumConstantNodes = new ArrayList<Node<V>>();
		ParseTreeUtils.collectNodes(node, new Predicate<Node<V>>() {

			public boolean apply(Node<V> input) {
				return "EnumConstantDeclaration".equals(input.getLabel());
			}
		}, enumConstantNodes);

		List<EnumConstantEntity> enumConstants = new ArrayList<EnumConstantEntity>();
		for (Node<V> enumConstantNode : enumConstantNodes) {
			Node<V> identifierNode = ParseTreeUtils.findNodeByLabel(enumConstantNode, "Identifier");
			EnumConstantEntity enumConstantEntity = new EnumConstantEntity();
			enumConstantEntity.setIdentifier(ParseTreeUtils.getNodeText(identifierNode, ast.inputBuffer).trim());

			Node<V> commentNode = ParseTreeUtils.findNodeByLabel(enumConstantNode, "DocumentingComment");
			if (commentNode != null) {
				enumConstantEntity.setCommentEntity(analyseCommentNode(commentNode));
			}

			enumConstants.add(enumConstantEntity);
		}

		return enumConstants;
	}

	private <V> CommentEntity analyseCommentNode(Node<V> node) {
		CommentEntity commentEntity = new CommentEntity();
		commentEntity.setComment(removeTrailingSpaces(ParseTreeUtils.getNodeText(node, ast.inputBuffer)));
		if (commentEntity.getComment().contains("\r") || commentEntity.getComment().contains("\n")) {
			commentEntity.setMultiline(true);
		}
		return commentEntity;
	}

	private String removeTrailingSpaces(String value) {
		char[] chars = value.toCharArray();
		for (int i = 0; i < chars.length; i++) {
			if (i != ' ') {
				return value.substring(i);
			}
		}
		return value;
	}
}
