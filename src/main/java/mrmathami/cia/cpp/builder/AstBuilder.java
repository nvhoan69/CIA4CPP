package mrmathami.cia.cpp.builder;

import mrmathami.cia.cpp.ast.IFunction;
import mrmathami.cia.cpp.ast.IVariable;
import mrmathami.cia.cpp.ast.*;
import mrmathami.util.Utilities;
import org.eclipse.cdt.core.dom.ast.*;
import org.eclipse.cdt.core.dom.ast.cpp.*;
import org.eclipse.cdt.internal.core.model.ASTStringUtil;

import java.util.*;

public final class AstBuilder {
	private final Map<String, INode> integralNodeMap = new HashMap<>();
	private final Map<IBinding, INode> bindingNodeMap = new HashMap<>();
	private final List<INode> unknownNodeList = new LinkedList<>();

	private AstBuilder() {
	}

	public static IRoot build(IASTTranslationUnit translationUnit) {
		return new AstBuilder().internalBuild(translationUnit);
	}

	private static void replaceNodeInsideNode(INode currentNode, INode oldNode, INode newNode) {
		if (currentNode instanceof ITypeContainer) {
			final ITypeContainer typeContainerNode = (ITypeContainer) currentNode;
			if (typeContainerNode.getType() == oldNode) {
				typeContainerNode.setType(newNode);
			}
		}
		if (currentNode instanceof IClass) {
			final IClass classNode = (IClass) currentNode;
			classNode.replaceBase(oldNode, newNode);
		}
		if (currentNode instanceof IFunction) {
			final IFunction functionNode = (IFunction) currentNode;
			functionNode.replaceParameter(oldNode, newNode);
		}
		currentNode.replaceChild(oldNode, newNode);
	}

	private void cleanUp(IRoot rootNode) {
		for (final INode node : bindingNodeMap.values()) {
			if (node instanceof IUnknown || node instanceof IIntegral) {
				// replace unknown node with integral node
				node.removeFromParent();
				node.removeChildren();
				node.removeAllDependency();
				if (node instanceof IUnknown) {
					final INode newNode = createIntegralNode(node.getName(), IntegralNode.builder());
					if (newNode != null) replaceNode(node, newNode);
				}
			} else {
				if (node instanceof IVariable) {
					// remove all children
					node.removeChildren();
					node.removeAllDependencyTo();

				} else if (node instanceof IFunction) {
					final IFunction function = (IFunction) node;
					final List<INode> parameters = List.copyOf(function.getParameters());
					final List<INode> variables = new ArrayList<>(function.getVariables());
					variables.removeAll(parameters);
					for (final INode variable : variables) {
						function.removeChild(variable);
						function.removeNodeDependencyTo(variable);
					}
				}
			}
		}
		for (final INode node : List.copyOf(unknownNodeList)) {
			// replace unknown node with integral node
			node.removeChildren();
			node.removeAllDependency();
			final INode newNode = createIntegralNode(node.getName(), IntegralNode.builder());
			if (newNode != null) replaceNode(node, newNode);
		}
		final List<INode> integralNodeList = List.copyOf(integralNodeMap.values());
		for (final INode node : integralNodeList) {
			node.removeFromParent();
			node.removeChildren();
			node.removeAllDependency();
		}
		rootNode.addIntegrals(integralNodeList);
	}

	private void createOverride(IRoot rootNode) {
		for (final IClass nodeClass : rootNode.getClasses()) {
			final List<INode> classBases = nodeClass.getBases();
			final List<IFunction> classFunctions = nodeClass.getFunctions();
			if (classBases.isEmpty() || classFunctions.isEmpty()) continue;

			final Queue<IClass> classQueue = new LinkedList<>();
			final Set<IFunction> classFunctionsMatched = new HashSet<>();

			for (final INode classBase : classBases) {
				if (classBase instanceof IClass) classQueue.add((IClass) classBase);
			}

			while (!classQueue.isEmpty() && classFunctions.size() != classFunctionsMatched.size()) {
				final IClass classBase = classQueue.poll();

				for (final INode baseBase : classBase.getBases()) {
					if (baseBase instanceof IClass) classQueue.add((IClass) baseBase);
				}

				final List<IFunction> baseFunctions = classBase.getFunctions();
				if (baseFunctions.isEmpty()) continue;

				for (final IFunction classFunction : classFunctions) {
					if (classFunctionsMatched.contains(classFunction)) continue;

					for (final IFunction baseFunction : baseFunctions) {
						if (!baseFunction.getName().equals(classFunction.getName())
								|| !Objects.equals(baseFunction.getType(), classFunction.getType())) continue;

						final List<INode> classParameters = classFunction.getParameters();
						final List<INode> baseParameters = baseFunction.getParameters();

						if (classParameters.size() != baseParameters.size()) continue;

						int i = -1;
						while (++i < classParameters.size()) {
							final INode classParameter = classParameters.get(i);
							final INode baseParameter = baseParameters.get(i);
							if (!classParameter.getClass().equals(baseParameter.getClass()) || classParameter instanceof IVariable
									&& !Objects.equals(((IVariable) classParameter).getType(), ((IVariable) baseParameter).getType())) {
								break;
							}
						}

						if (i >= classParameters.size()) {
							classFunction.addDependencyTo(baseFunction, DependencyType.OVERRIDE);
							classFunctionsMatched.add(classFunction);
							break;
						}
					}
				}
			}
		}
	}

	private void calculateDirectWeight(IRoot rootNode) {
		for (final INode node : rootNode) {
			float directWeight = 0.0f;
			for (final INode dependencyNode : node.getAllDependencyTo()) {
				for (final Map.Entry<DependencyType, Integer> entry : node.getNodeDependencyTo(dependencyNode).entrySet()) {
					directWeight += entry.getKey().getForwardWeight() * entry.getValue();
				}
			}
			node.setDirectWeight(directWeight);
		}
	}

	private void calculateIndirectWeight(IRoot rootNode) {
//		for (final INode node : rootNode) {
//			float indirectWeight = 0.0f;
//			for (final Map.Entry<INode, Dependency> entry : node.getDependency().entrySet()) {
//				final Dependency dependency = entry.getValue();
//				indirectWeight += dependency.getType().getWeight() * dependency.getCount() * entry.getKey().getDirectWeight();
//			}
//			node.setIndirectWeight(indirectWeight);
//		}
	}

	private IRoot internalBuild(IASTTranslationUnit translationUnit) {
		final IRoot rootNode = RootNode.builder().build();
		for (final IASTDeclaration declaration : translationUnit.getDeclarations()) {
			createChildrenFromDeclaration(rootNode, declaration);
		}

		cleanUp(rootNode);
		createOverride(rootNode);

		calculateDirectWeight(rootNode);
		calculateIndirectWeight(rootNode);
		return rootNode;
	}

	private void replaceNode(INode oldNode, INode newNode) {
		for (final Map.Entry<IBinding, INode> entry : bindingNodeMap.entrySet()) {
			final INode currentNode = entry.getValue();
			replaceNodeInsideNode(currentNode, oldNode, newNode);
			if (currentNode == oldNode) entry.setValue(newNode);
		}
		for (final ListIterator<INode> iterator = unknownNodeList.listIterator(); iterator.hasNext(); ) {
			final INode node = iterator.next();
			if (node != oldNode) {
				replaceNodeInsideNode(node, oldNode, newNode);
			} else {
				iterator.remove();
			}
		}
		if (newNode instanceof IIntegral) {
			newNode.removeFromParent();
			oldNode.removeChildren();
			oldNode.removeAllDependency();
		} else {
			newNode.addChildren(oldNode.removeChildren());
			oldNode.transferAllDependency(newNode);
		}
	}

	private <E extends INode, B extends INode.INodeBuilder<E, B>>
	INode createIntegralNode(String typeName, B builder) {
		if (typeName.isBlank()) return null;

		final INode existNode = integralNodeMap.get(typeName);
		if (existNode != null) return existNode;

		final INode newNode = builder
				.setName(typeName)
				.setUniqueName(typeName)
				.setSignature(typeName)
				.build();

		integralNodeMap.put(typeName, newNode);
		return newNode;
	}

	private <E extends INode, B extends INode.INodeBuilder<E, B>>
	INode createNode(IBinding binding, IASTName astName, String signature, B builder) {
		if (binding instanceof ICPPSpecialization) {
			binding = ((ICPPSpecialization) binding).getSpecializedBinding();
		}

		final INode existNode = bindingNodeMap.get(binding);
		if (existNode != null && (!(existNode instanceof IUnknown) || builder instanceof IUnknown.IUnknownBuilder)) {
			return existNode;
		}

		final String name = astName != null ? astName.toString() : binding != null ? binding.getName() : "";
		final String uniqueName = binding instanceof ICPPBinding
				? ASTTypeUtil.getQualifiedName((ICPPBinding) binding).replaceAll("^\\{ROOT:\\d+}", "{ROOT}")
				: astName != null ? ASTStringUtil.getQualifiedName(astName) : name;

		final INode newNode = builder instanceof IUnknown.IUnknownBuilder && binding instanceof IProblemBinding
				? createIntegralNode(uniqueName, IntegralNode.builder())
				: uniqueName.isBlank() && signature != null && !signature.isBlank()
				? createIntegralNode(signature, IntegralNode.builder())
				: builder.setName(name).setUniqueName(uniqueName).setSignature(signature != null ? signature : uniqueName).build();

		if (existNode != null) replaceNode(existNode, newNode);

		if (newNode instanceof IUnknown) unknownNodeList.add(newNode);
		if (binding != null) bindingNodeMap.put(binding, newNode);
		return newNode;
	}

	private INode createFromDeclarator(INode typeNode, IASTDeclarator declarator) {
		final IASTName declaratorName = declarator.getName();
		final IBinding declaratorBinding = declaratorName.resolveBinding();
		final String signature = ASTStringUtil.getSignatureString(declarator);

		if (declarator instanceof ICPPASTFunctionDeclarator) {
			// region
			final ICPPASTFunctionDeclarator functionDeclarator = (ICPPASTFunctionDeclarator) declarator;

			final INode functionNode = createNode(declaratorBinding, declaratorName, signature,
					FunctionNode.builder().setType(typeNode));

			if (functionNode instanceof IFunction) {
				for (final ICPPASTParameterDeclaration functionParameter : functionDeclarator.getParameters()) {
					final IASTDeclSpecifier parameterSpecifier = functionParameter.getDeclSpecifier();
					final ICPPASTDeclarator parameterDeclarator = functionParameter.getDeclarator();

					final INode parameterType = createFromDeclSpecifier(typeNode, parameterSpecifier);
					final INode parameterNode = createFromDeclarator(parameterType, parameterDeclarator);

					((IFunction) functionNode).addParameter(parameterNode);
					functionNode.addDependencyTo(parameterNode, DependencyType.MEMBER);
					functionNode.addDependencyTo(parameterType, DependencyType.USE);
				}
			}
			// endregion
			return functionNode;
		} else if (declarator instanceof ICPPASTDeclarator) {
			// region
			//noinspection UnnecessaryLocalVariable
			final INode node = createNode(declaratorBinding, declaratorName, signature, VariableNode.builder().setType(typeNode));
			// endregion
			return node;
		} else {
			// todo: debug?
			throw new IllegalArgumentException("createFromDeclarator(typeNode = (" + Utilities.objectToString(typeNode)
					+ "), declarator = (" + Utilities.objectToString(declarator) + "))");
		}
	}

	private INode createFromDeclSpecifier(INode parentNode, IASTDeclSpecifier declSpecifier) {
		final String signature = ASTStringUtil.getSignatureString(declSpecifier, null);

		if (declSpecifier instanceof ICPPASTEnumerationSpecifier) {
			// region
			final ICPPASTEnumerationSpecifier enumerationSpecifier = (ICPPASTEnumerationSpecifier) declSpecifier;
			final IASTName enumerationName = enumerationSpecifier.getName();
			final IBinding enumerationBinding = enumerationName.resolveBinding();

			final INode enumNode = createNode(enumerationBinding, enumerationName, signature, EnumNode.builder());

			final INode nodeType = enumerationSpecifier.isScoped() ? enumNode : null;
			for (final IASTEnumerationSpecifier.IASTEnumerator enumerator : enumerationSpecifier.getEnumerators()) {
				final IASTName enumeratorName = enumerator.getName();
				final IBinding enumeratorBinding = enumeratorName.resolveBinding();

				final INode enumeratorNode = createNode(enumeratorBinding, enumeratorName, null, VariableNode.builder().setType(nodeType));

				enumNode.addChild(enumeratorNode);
				enumNode.addDependencyTo(enumeratorNode, DependencyType.MEMBER);
			}
			// endregion
			parentNode.addChild(enumNode);
			parentNode.addDependencyTo(enumNode, DependencyType.MEMBER);
			return enumNode;
		} else if (declSpecifier instanceof ICPPASTCompositeTypeSpecifier) {
			// region
			final ICPPASTCompositeTypeSpecifier classSpecifier = (ICPPASTCompositeTypeSpecifier) declSpecifier;
			final IASTName className = classSpecifier.getName();
			final IBinding classBinding = className.resolveBinding();

			final INode classNode = createNode(classBinding, className, signature, ClassNode.builder());

			if (classNode instanceof IClass) {
				for (final ICPPASTCompositeTypeSpecifier.ICPPASTBaseSpecifier classBaseSpecifier : classSpecifier.getBaseSpecifiers()) {
					final ICPPASTNameSpecifier classBaseNameSpecifier = classBaseSpecifier.getNameSpecifier();
					final IBinding classBaseNameBinding = classBaseNameSpecifier.resolveBinding();

					final INode classBaseNode = createNode(classBaseNameBinding, null, null, UnknownNode.builder());
					((IClass) classNode).addBase(classBaseNode);
					classNode.addDependencyTo(classBaseNode, DependencyType.INHERITANCE);
				}
			}

			for (final IASTDeclaration classChildDeclaration : classSpecifier.getDeclarations(false)) {
				createChildrenFromDeclaration(classNode, classChildDeclaration);
			}
			// endregion
			parentNode.addChild(classNode);
			parentNode.addDependencyTo(classNode, DependencyType.MEMBER);
			return classNode;
		} else if (declSpecifier instanceof ICPPASTNamedTypeSpecifier) {
			// region
			final IASTNamedTypeSpecifier namedSpecifier = (IASTNamedTypeSpecifier) declSpecifier;
			final IASTName namedName = namedSpecifier.getName();
			final IBinding namedBinding = namedName.resolveBinding();

			//noinspection UnnecessaryLocalVariable
			final INode namedNode = createNode(namedBinding, namedName, signature, UnknownNode.builder());
			// endregion
			return namedNode;
		} else if (declSpecifier instanceof ICPPASTElaboratedTypeSpecifier) {
			// region
			final ICPPASTElaboratedTypeSpecifier elaboratedSpecifier = (ICPPASTElaboratedTypeSpecifier) declSpecifier;
			final IASTName elaboratedName = elaboratedSpecifier.getName();
			final IBinding elaboratedBinding = elaboratedName.resolveBinding();

			switch (elaboratedSpecifier.getKind()) {
				case IASTElaboratedTypeSpecifier.k_enum:
					return createNode(elaboratedBinding, elaboratedName, signature, EnumNode.builder());
				case IASTElaboratedTypeSpecifier.k_struct:
				case IASTElaboratedTypeSpecifier.k_union:
				case ICPPASTElaboratedTypeSpecifier.k_class:
					return createNode(elaboratedBinding, elaboratedName, signature, ClassNode.builder());
			}

			//noinspection UnnecessaryLocalVariable
			final INode elaboratedNode = createNode(elaboratedBinding, elaboratedName, signature, UnknownNode.builder());
			// endregion
			return elaboratedNode;
		} else if (declSpecifier instanceof ICPPASTSimpleDeclSpecifier) {
			// region
			//noinspection UnnecessaryLocalVariable
			final INode simpleNode = createIntegralNode(signature, IntegralNode.builder());
			// endregion
			return simpleNode;
		} else {
			// todo: debug?
			throw new IllegalArgumentException("createFromDeclSpecifier(declSpecifier = (" + Utilities.objectToString(declSpecifier) + "))");
		}
	}

	private INode createFromTemplateParameter(INode parentNode, ICPPASTTemplateParameter templateParameter) {
		if (templateParameter instanceof ICPPASTParameterDeclaration) {
			final ICPPASTParameterDeclaration parameterDeclaration = (ICPPASTParameterDeclaration) templateParameter;

			final IASTDeclSpecifier parameterSpecifier = parameterDeclaration.getDeclSpecifier();
			final ICPPASTDeclarator parameterDeclarator = parameterDeclaration.getDeclarator();

			final INode parameterType = createFromDeclSpecifier(parentNode, parameterSpecifier);
			//noinspection UnnecessaryLocalVariable
			final INode parameterNode = createFromDeclarator(parameterType, parameterDeclarator);

			return parameterNode;

		} else if (templateParameter instanceof ICPPASTSimpleTypeTemplateParameter) {
			final ICPPASTSimpleTypeTemplateParameter simpleParameter = (ICPPASTSimpleTypeTemplateParameter) templateParameter;
			final IASTName simpleName = simpleParameter.getName();
			final IBinding simpleBinding = simpleName.resolveBinding();

			//noinspection UnnecessaryLocalVariable
			final INode simpleNode = createNode(simpleBinding, simpleName, null, VariableNode.builder());

			return simpleNode;
		} else if (templateParameter instanceof ICPPASTTemplatedTypeTemplateParameter) {
			final ICPPASTTemplatedTypeTemplateParameter nestedTemplateParameter = (ICPPASTTemplatedTypeTemplateParameter) templateParameter;
			final IASTName nestedTemplateName = nestedTemplateParameter.getName();
			final IBinding nestedTemplateBinding = nestedTemplateName.resolveBinding();
			final INode nestedTemplateNode = createNode(nestedTemplateBinding, nestedTemplateName,
					null, VariableNode.builder());

			for (final ICPPASTTemplateParameter nestedParameter : nestedTemplateParameter.getTemplateParameters()) {
				final INode nestedNode = createFromTemplateParameter(nestedTemplateNode, nestedParameter);
				nestedTemplateNode.addChild(nestedNode);
				nestedTemplateNode.addDependencyTo(nestedNode, DependencyType.MEMBER);
			}

			return nestedTemplateNode;
		} else {
			// todo: debug?
			throw new IllegalArgumentException("createFromTemplateParameter(parentNode = (" + Utilities.objectToString(parentNode)
					+ "), templateParameter = (" + Utilities.objectToString(templateParameter) + "))");
		}
	}

	private List<INode> createChildrenFromDeclaration(INode parentNode, IASTDeclaration declaration) {
		if (declaration instanceof ICPPASTVisibilityLabel
				|| declaration instanceof ICPPASTUsingDeclaration
				|| declaration instanceof ICPPASTUsingDirective
				|| declaration instanceof ICPPASTNamespaceAlias
				|| declaration instanceof IASTProblemDeclaration
				|| declaration instanceof ICPPASTStaticAssertDeclaration
				|| declaration instanceof ICPPASTExplicitTemplateInstantiation
		) {
			// skipped
			return List.of();

		} else if (declaration instanceof ICPPASTLinkageSpecification) {
			final ICPPASTLinkageSpecification linkageSpecification = (ICPPASTLinkageSpecification) declaration;
			final List<INode> childrenNode = new ArrayList<>();
			for (final IASTDeclaration linkageDeclaration : linkageSpecification.getDeclarations(false)) {
				childrenNode.addAll(createChildrenFromDeclaration(parentNode, linkageDeclaration));
			}
			return childrenNode;

		} else if (declaration instanceof ICPPASTNamespaceDefinition) {
			// region
			final ICPPASTNamespaceDefinition namespaceDefinition = (ICPPASTNamespaceDefinition) declaration;
			final IASTName namespaceName = namespaceDefinition.getName();
			final IBinding namespaceBinding = namespaceName.resolveBinding();

			final INode namespaceNode = createNode(namespaceBinding, namespaceName, null, NamespaceNode.builder());
			final IASTDeclaration[] namespaceChildDeclarations = namespaceDefinition.getDeclarations(false);
			for (final IASTDeclaration namespaceChildDeclaration : namespaceChildDeclarations) {
				createChildrenFromDeclaration(namespaceNode, namespaceChildDeclaration);
			}
			// endregion
			parentNode.addChild(namespaceNode);
			parentNode.addDependencyTo(namespaceNode, DependencyType.MEMBER);
			return List.of(namespaceNode);

		} else if (declaration instanceof IASTSimpleDeclaration) {
			// region
			final IASTSimpleDeclaration simpleDeclaration = (IASTSimpleDeclaration) declaration;

			final IASTDeclSpecifier simpleSpecifier = simpleDeclaration.getDeclSpecifier();
			final INode simpleNodeType = createFromDeclSpecifier(parentNode, simpleSpecifier);

			if (simpleNodeType != null) {
				if (!(simpleNodeType instanceof IUnknown || simpleNodeType instanceof IIntegral)
						&& simpleNodeType.getParent() == null && parentNode != simpleNodeType) {
					parentNode.addChild(simpleNodeType);
					parentNode.addDependencyTo(simpleNodeType, DependencyType.MEMBER);
				} else {
					parentNode.addDependencyTo(simpleNodeType, DependencyType.USE);
				}
			}

			final List<INode> simpleNodeList = new ArrayList<>();
			for (final IASTDeclarator simpleDeclarator : simpleDeclaration.getDeclarators()) {
				final INode simpleNode = createFromDeclarator(simpleNodeType, simpleDeclarator);
				simpleNodeList.add(simpleNode);
				parentNode.addChild(simpleNode);
				parentNode.addDependencyTo(simpleNode, DependencyType.MEMBER);
			}
			// endregion
			return simpleNodeList.size() > 0 ? simpleNodeList : simpleNodeType != null ? List.of(simpleNodeType) : List.of();

		} else if (declaration instanceof ICPPASTFunctionDefinition) {
			// region
			final ICPPASTFunctionDefinition functionDefinition = (ICPPASTFunctionDefinition) declaration;

			final IASTDeclSpecifier functionSpecifier = functionDefinition.getDeclSpecifier();
			final IASTFunctionDeclarator functionDeclarator = functionDefinition.getDeclarator();

			final INode functionReturnType = createFromDeclSpecifier(parentNode, functionSpecifier);
			final INode functionNode = createFromDeclarator(functionReturnType, functionDeclarator);

			if (functionReturnType != null) functionNode.addDependencyTo(functionReturnType, DependencyType.USE);

			// function dependency
			for (final ICPPASTConstructorChainInitializer memberChainInitializer : functionDefinition.getMemberInitializers()) {
				final IASTName memberName = memberChainInitializer.getMemberInitializerId();
				final IBinding memberBinding = memberName.resolveBinding();

				final INode memberNode = createNode(memberBinding, memberName, null, UnknownNode.builder());
				functionNode.addDependencyTo(memberNode, DependencyType.MEMBER);

				final IASTInitializer memberInitializer = memberChainInitializer.getInitializer();
				createChildrenFromAstNode(functionNode, memberInitializer);
			}

			final IASTStatement functionBody = functionDefinition.getBody();
			if (functionBody != null) createChildrenFromAstNode(functionNode, functionBody);

			parentNode.addChild(functionNode);
			parentNode.addDependencyTo(functionNode, DependencyType.MEMBER);
			return List.of(functionNode);
			// endregion

		} else if (declaration instanceof ICPPASTTemplateDeclaration) {
			final ICPPASTTemplateDeclaration templateDeclaration = (ICPPASTTemplateDeclaration) declaration;

			final IASTDeclaration innerDeclaration = templateDeclaration.getDeclaration();
			final List<INode> innerNodeList = createChildrenFromDeclaration(parentNode, innerDeclaration);

			for (final INode innerNode : innerNodeList) {
				parentNode.addChild(innerNode);
				parentNode.addDependencyTo(innerNode, DependencyType.MEMBER);
			}

			final INode innerNode = innerNodeList.get(0);

			for (final ICPPASTTemplateParameter templateParameter : templateDeclaration.getTemplateParameters()) {
				final INode templateNode = createFromTemplateParameter(innerNode, templateParameter);
				innerNode.addChild(templateNode);
				innerNode.addDependencyTo(templateNode, DependencyType.MEMBER);
			}
			return innerNodeList;

		} else if (declaration instanceof ICPPASTAliasDeclaration) {
			// region
			final ICPPASTAliasDeclaration aliasDefinition = (ICPPASTAliasDeclaration) declaration;
			final IASTName aliasName = aliasDefinition.getAlias();
			final IBinding aliasBinding = aliasName.resolveBinding();

			final ICPPASTTypeId aliasTypeId = aliasDefinition.getMappingTypeId();

			final IASTDeclSpecifier aliasDeclSpecifier = aliasTypeId.getDeclSpecifier();
			final INode aliasType = createFromDeclSpecifier(parentNode, aliasDeclSpecifier);

			final IASTDeclarator aliasDeclarator = aliasTypeId.getAbstractDeclarator();
			final INode aliasNodeType = createFromDeclarator(aliasType, aliasDeclarator);

			final INode aliasNode = createNode(aliasBinding, aliasName,
					ASTStringUtil.getSignatureString(aliasDeclSpecifier, aliasDeclarator),
					VariableNode.builder().setType(aliasNodeType));

			// endregion
			parentNode.addChild(aliasNode);
			parentNode.addDependencyTo(aliasNode, DependencyType.MEMBER);
			parentNode.addDependencyTo(aliasType, DependencyType.USE);
			parentNode.addDependencyTo(aliasNodeType, DependencyType.USE);
			return List.of(aliasNode);

		} else {
			// todo: debug?
			throw new IllegalArgumentException("createChildrenFromDeclaration(parentNode = (" + Utilities.objectToString(parentNode)
					+ "), declaration = (" + Utilities.objectToString(declaration) + "))");
		}
	}

	private void createChildrenFromAstNode(INode parentNode, IASTNode astNode) {
		for (final IASTNode astChild : astNode.getChildren()) {
			if (astChild instanceof IASTDeclaration) {
				createChildrenFromDeclaration(parentNode, (IASTDeclaration) astChild);
			} else if (astChild instanceof IASTName) {
				final IASTName astName = (IASTName) astChild;
				final IBinding astBinding = astName.resolveBinding();

				final INode childNode = createNode(astBinding, astName, null, UnknownNode.builder());
				if (!(childNode instanceof IIntegral)) {
					parentNode.addDependencyTo(childNode, childNode instanceof IFunction
							? DependencyType.INVOCATION : DependencyType.USE);
				}
			} else {
				createChildrenFromAstNode(parentNode, astChild);
			}
		}
	}
}

