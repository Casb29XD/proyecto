package com.analisis.proyecto.servicio;

import com.analisis.proyecto.modelo.Articulo;
import com.analisis.proyecto.modelo.ClusterNode;
import com.analisis.proyecto.servicio.similitud.SimilitudAlgoritmo;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

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
}
