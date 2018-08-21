package ru.shemplo.lecture.graphs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.Stack;
import java.util.StringTokenizer;

public class Graph {
 
    public static void main (String [] args) throws Exception {
        // ������ ������, ������� ������������ ����� � �������� �������
        File file = new File ("graph.in");
        
        // ������ ������ ���������, ������� ����� �������� �����
        InputStream is = new FileInputStream (file);
        // ������ ������ ��������, ������� ����� ����� �� 2 ����� � ���������� �� � �������
        Reader r = new InputStreamReader (is, StandardCharsets.UTF_8);
        // ������ ������ ��������, ������� ����� �������� ��������� ��� ������
        BufferedReader br = new BufferedReader (r);
        
        // ��������� ������ ������ � ����������� �����
        String line = br.readLine ();
        // ��������� ������ �� �����
        StringTokenizer st = new StringTokenizer (line);
        int nodes = Integer.parseInt (st.nextToken ()), // ���������� ������
            edges = Integer.parseInt (st.nextToken ()); // ���������� ����
        
        // ������ nodes ������
        for (int i = 0; i < nodes; i++) {
            graph.add (new Node ());
        }
        
        // ��������� edges ���� � �������� �� � ����
        for (int i = 0; i < edges; i++) {
            // ������� ������
            line = br.readLine ();
            // ������� � �� �����
            st = new StringTokenizer (line);
            int from = Integer.parseInt (st.nextToken ()) - 1, // ������ �������
                  to = Integer.parseInt (st.nextToken ()) - 1; // ������ �������
            // ��������� ���� � �������
            graph.get (from).addEdge (to);
            graph.get (to).addEdge (from);
        }
        
        // ��������� ��������, �.�. ����� �� ����� �����
        br.close ();
        
        ///////////////////////////////////////
        
        // ������� � ������� ������� ������� � �������� 0
        System.out.println ("Degree of node 1 is " + graph.get (0).getDegree ());
        
        ///////////////////////////////////////
        
        // ������� ��������� ������� ������ �����
        int summaryDegree = 0;
        // ���������� � ����������� ������� ������ �������
        for (int i = 0; i < nodes; i++) {
            summaryDegree += graph.get (i).getDegree ();
        }
        // ������� � ������� ��������� ������� ������ �����
        System.out.println ("Summary degree is " + summaryDegree);
        // ���������, ��� ����� �������� ������ �������� ������ ������
        System.out.println ("Summary degree is even " + (summaryDegree % 2 == 0));
        
        // DFS: depth ... search
        
        // ��������� ��������� �����
        Random random = new Random ();
        // �������� ������������ ��������� �������
        int startNode = random.nextInt (nodes);
        // ������� � ������� ������ ��������� �������
        System.out.println ("Random nuber " + startNode);
        
        // ������ ���������, � ������� ����� ������� ������� ���������� ������
        Set <Integer> visited = new HashSet <> ();
        // ������ ����, � ������� ����� ��������� ������� ��� ��������� ������
        Stack <Integer> stack = new Stack <> ();
        // ��������� ��������� ������� � ������� � �������� � ��� ����������
        visited.add (startNode);
        stack.push (startNode);
        
        // ������� ������� ���������� ���������� ������
        int numberOfVisited = 0;
        // ���� ������� ������ ����� �� ������, ��������� ��������
        while (!stack.isEmpty ()) {
            // ������� �� ����� ����� - �������, ������� ������ ��������
            int node = stack.pop ();
            // �������� ������� - ���������� ������ ����������� �� 1
            numberOfVisited++;
            
            // ������� ������ ������, � �������� ������ ������� �������
            List <Integer> neighbours = graph.get (node).getNeighbours ();
            // ��� ������� ������ ������� ���������, ��� �� �� �������
            for (int i = 0; i < neighbours.size (); i++) {
                int neighbour = neighbours.get (i);
                // ���� ����� �� �������, �� �������� ��� � �������
                // � �������� ��� ��� �����������
                if (!visited.contains (neighbour)) {
                    visited.add (neighbour);
                    stack.add (neighbour);
                }
            }
        }
        
        // ��������: ���� ��� ������ ����� ���������� ���������� ������
        // ����� ���������� ������ � �����, �� ���� ���� �������
        if (nodes == numberOfVisited) {
            System.out.println ("Graph is linked");
        } else {
            System.out.println ("Graph is not linked");
        }
    }
    
    public static List <Node> graph = new ArrayList <> ();
    
    public static class Node {
        
        // ������ ���� �� ������ ������� � �����-�� ������
        private List <Integer> edges = new ArrayList <> ();
        
        // �����, ������� ��������� �������� ����� �� ���� �������
        public void addEdge (int node) {
            edges.add (node);
        }
        
        // �����, ������� ���������� ������� �������
        public Integer getDegree () {
            return edges.size ();
        }
        
        // �����, ������� ���������� ������� �������� ������
        public List <Integer> getNeighbours () {
            return edges;
        }
        
    }
    
}
