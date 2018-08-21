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
        // Создаём объект, который соотвествует файлу в файловой системе
        File file = new File ("graph.in");
        
        // Создаём объект конвейера, который будет выдавать байты
        InputStream is = new FileInputStream (file);
        // Создаём объект читателя, который будет брать по 2 байта и превращать их в символы
        Reader r = new InputStreamReader (is, StandardCharsets.UTF_8);
        // Созбаём объект читателя, который будет ситывать полностью всю строку
        BufferedReader br = new BufferedReader (r);
        
        // Считываем первую строку с параметрами графа
        String line = br.readLine ();
        // Разбиваем строку на слова
        StringTokenizer st = new StringTokenizer (line);
        int nodes = Integer.parseInt (st.nextToken ()), // Количество вершин
            edges = Integer.parseInt (st.nextToken ()); // Количество рёбер
        
        // Создаём nodes вершин
        for (int i = 0; i < nodes; i++) {
            graph.add (new Node ());
        }
        
        // Считываем edges ребёр и добавлем их в граф
        for (int i = 0; i < edges; i++) {
            // Считали строку
            line = br.readLine ();
            // Разбили её на слова
            st = new StringTokenizer (line);
            int from = Integer.parseInt (st.nextToken ()) - 1, // Первая вершина
                  to = Integer.parseInt (st.nextToken ()) - 1; // Вторая вершина
            // Добавляем рёбра в вершины
            graph.get (from).addEdge (to);
            graph.get (to).addEdge (from);
        }
        
        // Закрываем конвейер, т.к. дошли до конца файла
        br.close ();
        
        ///////////////////////////////////////
        
        // Выводим в консоль степень вершины с индексом 0
        System.out.println ("Degree of node 1 is " + graph.get (0).getDegree ());
        
        ///////////////////////////////////////
        
        // Считаем суммарную степень вершин графа
        int summaryDegree = 0;
        // Прибавляем в аккумулятор степень каждой вершины
        for (int i = 0; i < nodes; i++) {
            summaryDegree += graph.get (i).getDegree ();
        }
        // Выводим в консоль суммарную степень вершин графа
        System.out.println ("Summary degree is " + summaryDegree);
        // Проверяем, что сумма степеней вершин является чётным числом
        System.out.println ("Summary degree is even " + (summaryDegree % 2 == 0));
        
        // DFS: depth ... search
        
        // Генератор случайных чисел
        Random random = new Random ();
        // Выбираем произвольную начальную вершину
        int startNode = random.nextInt (nodes);
        // Выводим в консоль индекс стартовой вершины
        System.out.println ("Random nuber " + startNode);
        
        // Создаём множество, в котором будем хранить индексы посещённых вершин
        Set <Integer> visited = new HashSet <> ();
        // Создаём стек, в котором будет храниться очередь для посещения вершин
        Stack <Integer> stack = new Stack <> ();
        // Добавляем стартовую вершину в очередь и помечаем её как посещённую
        visited.add (startNode);
        stack.push (startNode);
        
        // Заводим счётчик количества посещённых вершин
        int numberOfVisited = 0;
        // Пока очередь обхода графа не пустая, выполняем действия
        while (!stack.isEmpty ()) {
            // Снимаем со стека число - вершина, которую сейчас посещаем
            int node = stack.pop ();
            // Посетили вершину - посещённых вершин увеличилось на 1
            numberOfVisited++;
            
            // Полуаем список вершин, с которыми данная вершина связана
            List <Integer> neighbours = graph.get (node).getNeighbours ();
            // Для каждого соседа вершины проверяем, что он не посещён
            for (int i = 0; i < neighbours.size (); i++) {
                int neighbour = neighbours.get (i);
                // Если сосед не посещён, то добавлем его в очередь
                // и отмечаем его как посещённого
                if (!visited.contains (neighbour)) {
                    visited.add (neighbour);
                    stack.add (neighbour);
                }
            }
        }
        
        // Предикат: если при обходе графа количество посещённых вершин
        // равно количеству вершин в графе, то этот граф связный
        if (nodes == numberOfVisited) {
            System.out.println ("Graph is linked");
        } else {
            System.out.println ("Graph is not linked");
        }
    }
    
    public static List <Node> graph = new ArrayList <> ();
    
    public static class Node {
        
        // Список ребёр из данной вершины в какие-то другие
        private List <Integer> edges = new ArrayList <> ();
        
        // Метод, который позволяет добавить ребро из этой вершины
        public void addEdge (int node) {
            edges.add (node);
        }
        
        // Метод, который возвращает степень вершины
        public Integer getDegree () {
            return edges.size ();
        }
        
        // Метод, который возвращает индексы соседних вершин
        public List <Integer> getNeighbours () {
            return edges;
        }
        
    }
    
}
