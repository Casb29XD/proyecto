package com.analisis.proyecto.modelo;

public class ClusterNode {
    private String id;        // Null for internal nodes
    private String label;     // Null for internal nodes
    private ClusterNode left;
    private ClusterNode right;
    private double distance;
    private int size;

    // Constructor for leaf node
    public ClusterNode(String id, String label) {
        this.id = id;
        this.label = label;
        this.left = null;
        this.right = null;
        this.distance = 0.0;
        this.size = 1;
    }

    // Constructor for internal node
    public ClusterNode(ClusterNode left, ClusterNode right, double distance) {
        this.id = null;
        this.label = null;
        this.left = left;
        this.right = right;
        this.distance = distance;
        this.size = left.getSize() + right.getSize();
    }

    public String getId() { return id; }
    public String getLabel() { return label; }
    public ClusterNode getLeft() { return left; }
    public ClusterNode getRight() { return right; }
    public double getDistance() { return distance; }
    public int getSize() { return size; }
    
    public boolean isLeaf() {
        return left == null && right == null;
    }
}
