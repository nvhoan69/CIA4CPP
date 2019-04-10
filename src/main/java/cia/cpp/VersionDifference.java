package cia.cpp;

import cia.cpp.ast.INode;
import mrmathami.util.ImmutablePair;

import java.io.*;
import java.util.Set;

public final class VersionDifference implements Serializable {
	private static final long serialVersionUID = 8631661649798016080L;

	private final Version versionA;
	private final Version versionB;
	private final Set<INode> addedNodes;
	private final Set<ImmutablePair<INode, INode>> changedNodes;
	private final Set<ImmutablePair<INode, INode>> unchangedNodes;
	private final Set<INode> removedNodes;

	private VersionDifference(Version versionA, Version versionB, Set<INode> addedNodes, Set<ImmutablePair<INode, INode>> changedNodes, Set<ImmutablePair<INode, INode>> unchangedNodes, Set<INode> removedNodes) {
		this.versionA = versionA;
		this.versionB = versionB;
		this.addedNodes = addedNodes;
		this.changedNodes = changedNodes;
		this.unchangedNodes = unchangedNodes;
		this.removedNodes = removedNodes;
	}

	public static VersionDifference of(Version versionA, Version versionB, Set<INode> addedNodes, Set<ImmutablePair<INode, INode>> changedNodes, Set<ImmutablePair<INode, INode>> unchangedNodes, Set<INode> removedNodes) {
		return new VersionDifference(versionA, versionB, addedNodes, changedNodes, unchangedNodes, removedNodes);
	}

	public static VersionDifference fromInputStream(InputStream inputStream) throws IOException {
		final ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
		try {
			return (VersionDifference) objectInputStream.readObject();
		} catch (ClassNotFoundException | ClassCastException e) {
			throw new IOException("Wrong input file format!", e);
		}
	}

	public final void toOutputStream(OutputStream outputStream) throws IOException {
		final ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
		objectOutputStream.writeObject(this);
		objectOutputStream.flush();
	}

	public final Version getVersionA() {
		return versionA;
	}

	public final Version getVersionB() {
		return versionB;
	}

	public final Set<INode> getAddedNodes() {
		return addedNodes;
	}

	public final Set<ImmutablePair<INode, INode>> getChangedNodes() {
		return changedNodes;
	}

	public final Set<ImmutablePair<INode, INode>> getUnchangedNodes() {
		return unchangedNodes;
	}

	public final Set<INode> getRemovedNodes() {
		return removedNodes;
	}
}