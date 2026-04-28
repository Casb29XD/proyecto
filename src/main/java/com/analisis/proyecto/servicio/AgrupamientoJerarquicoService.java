package com.analisis.proyecto.servicio;

import com.analisis.proyecto.modelo.Articulo;
import com.analisis.proyecto.modelo.ClusterNode;
import com.analisis.proyecto.servicio.similitud.SimilitudAlgoritmo;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Service
public class AgrupamientoJerarquicoService {

    private final List<SimilitudAlgoritmo> algoritmos;

    public AgrupamientoJerarquicoService(List<SimilitudAlgoritmo> algoritmos) {
        this.algoritmos = algoritmos;
    }

    public ClusterNode agrupar(List<Articulo> articulos, String linkageMethod, String metricName) {
        if (articulos == null || articulos.isEmpty()) {
            return null;
        }
        if (articulos.size() == 1) {
            return new ClusterNode(articulos.get(0).getId(), articulos.get(0).getTitulo());
        }

        int n = articulos.size();
        List<ClusterNode> activeClusters = new ArrayList<>();
        
        for (Articulo articulo : articulos) {
            activeClusters.add(new ClusterNode(articulo.getId(), articulo.getTitulo()));
        }

        SimilitudAlgoritmo metric = algoritmos.stream()
                .filter(a -> a.getNombreAlgoritmo().equalsIgnoreCase(metricName))
                .findFirst()
                .orElse(algoritmos.get(0));

        double[][] distances = new double[n * 2][n * 2]; 
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                double sim = metric.calcularSimilitud(articulos.get(i).getResumen(), articulos.get(j).getResumen());
                double dist = Math.max(0.0, 1.0 - sim); 
                distances[i][j] = dist;
                distances[j][i] = dist;
            }
        }

        List<Integer> activeIndices = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            activeIndices.add(i);
        }

        int nextIndex = n;

        while (activeClusters.size() > 1) {
            double minDistance = Double.MAX_VALUE;
            int mergeIdxA = -1;
            int mergeIdxB = -1;
            int listIdxA = -1;
            int listIdxB = -1;

            for (int i = 0; i < activeIndices.size(); i++) {
                for (int j = i + 1; j < activeIndices.size(); j++) {
                    int idx1 = activeIndices.get(i);
                    int idx2 = activeIndices.get(j);
                    if (distances[idx1][idx2] < minDistance) {
                        minDistance = distances[idx1][idx2];
                        mergeIdxA = idx1;
                        mergeIdxB = idx2;
                        listIdxA = i;
                        listIdxB = j;
                    }
                }
            }

            ClusterNode nodeA = activeClusters.get(listIdxA);
            ClusterNode nodeB = activeClusters.get(listIdxB);
            ClusterNode parent = new ClusterNode(nodeA, nodeB, minDistance);

            int newIdx = nextIndex++;
            for (int k : activeIndices) {
                if (k == mergeIdxA || k == mergeIdxB) continue;
                
                double d1 = distances[mergeIdxA][k];
                double d2 = distances[mergeIdxB][k];
                double newDist = 0.0;

                switch (linkageMethod.toLowerCase()) {
                    case "single":
                        newDist = Math.min(d1, d2);
                        break;
                    case "complete":
                        newDist = Math.max(d1, d2);
                        break;
                    case "average":
                    default:
                        int sizeA = nodeA.getSize();
                        int sizeB = nodeB.getSize();
                        newDist = ((d1 * sizeA) + (d2 * sizeB)) / (double) (sizeA + sizeB);
                        break;
                }
                
                distances[newIdx][k] = newDist;
                distances[k][newIdx] = newDist;
            }

            if (listIdxA > listIdxB) {
                activeClusters.remove(listIdxA);
                activeClusters.remove(listIdxB);
                activeIndices.remove(listIdxA);
                activeIndices.remove(listIdxB);
            } else {
                activeClusters.remove(listIdxB);
                activeClusters.remove(listIdxA);
                activeIndices.remove(listIdxB);
                activeIndices.remove(listIdxA);
            }

            activeClusters.add(parent);
            activeIndices.add(newIdx);
        }

        return activeClusters.get(0);
    }

    public static class ComparacionMetodo {
        private String metodo;
        private double score;
        private String descripcion;

        public ComparacionMetodo(String metodo, double score, String descripcion) {
            this.metodo = metodo;
            this.score = score;
            this.descripcion = descripcion;
        }

        public String getMetodo() { return metodo; }
        public double getScore() { return score; }
        public String getDescripcion() { return descripcion; }
    }

    public List<ComparacionMetodo> compararMetodos(List<Articulo> articulos, String metricName) {
        List<ComparacionMetodo> comparacion = new ArrayList<>();
        
        if (articulos == null || articulos.size() < 2) {
            return comparacion;
        }

        String[] metodos = {"single", "average", "complete"};
        
        for (String metodo : metodos) {
            ClusterNode root = agrupar(articulos, metodo, metricName);
            double score = calcularCohesion(root);
            
            String desc = "";
            if (metodo.equals("single")) desc = "Propenso a efecto cadena (chaining). Une por similitud máxima local.";
            if (metodo.equals("average")) desc = "Balanceado. Considera la estructura global del clúster (UPGMA).";
            if (metodo.equals("complete")) desc = "Fuerza clústeres esféricos y compactos. Une por similitud mínima.";
            
            comparacion.add(new ComparacionMetodo(metodo, score, desc));
        }
        
        // Normalizar scores para que el mejor sea 100% y los demás proporcionales
        double maxScore = comparacion.stream().mapToDouble(ComparacionMetodo::getScore).max().orElse(1.0);
        if (maxScore == 0) maxScore = 1.0;
        
        for (ComparacionMetodo c : comparacion) {
            c.score = Math.round((c.score / maxScore) * 100.0 * 10.0) / 10.0;
        }

        return comparacion;
    }

    private double calcularCohesion(ClusterNode node) {
        if (node == null || node.isLeaf()) return 0.0;
        
        List<Double> distancias = new ArrayList<>();
        recolectarDistancias(node, distancias);
        
        if (distancias.isEmpty()) return 0.0;
        
        // Una menor distancia promedio de fusión significa clústeres más densos/cohesivos
        double sum = 0;
        for (Double d : distancias) sum += d;
        double avgDist = sum / distancias.size();
        
        // Transformar distancia en "score de cohesión" (mayor es mejor)
        return Math.max(0.0, 1.0 - avgDist);
    }
    
    private void recolectarDistancias(ClusterNode node, List<Double> distancias) {
        if (node == null || node.isLeaf()) return;
        distancias.add(node.getDistance());
        recolectarDistancias(node.getLeft(), distancias);
        recolectarDistancias(node.getRight(), distancias);
    }
}
