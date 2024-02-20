package org.example;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

public class Main {
    public static void main(String[] args) {
//        https://github.com/PeacockTeam/new-job/releases/download/v1.0/lng-4.txt.gz

        long start = System.nanoTime();
        String pathIn = "C://Users//alkor//Downloads//lng-4.txt.gz";
        String outname = "C://Users//alkor//Downloads//lng-4-group.txt";
        List<String> scanList = new ArrayList<>();
        File file = new File(pathIn);
        List<List<Long>> inputList = new ArrayList<>(); //быстрый способ создать коллекцию
        int maxL = 0; //максимальная длина строки (пока для справки)
        int iter = 0; //Сколько строк прочитали на входе (тоже для справки)

        try (GZIPInputStream gzip = new GZIPInputStream(new FileInputStream(file));
             BufferedReader br = new BufferedReader(new InputStreamReader(gzip));) {
            String line = null;
            while ((line = br.readLine()) != null) {
                scanList = Arrays.asList(line.split(";"));
                try {
                    List<Long> result = new ArrayList<>();
                    boolean isPhone = false;
                    for (int i = 0; i < scanList.size(); i++) {
                        String x = scanList.get(i).replaceAll("[^0-9]", "");
                        Long val = (x.length() > 5 ? Long.valueOf(x) : 0L);
                        result.add(val);
                        isPhone = val > 0;
                    }
                    iter++;
                    if (isPhone) {
                        if (result.size() > maxL) maxL = result.size();
                        inputList.add(result);
                    }
                } catch (NumberFormatException e) {
                    continue; // "бракованные" строки пропускаем
                }
            }
            int size0 = inputList.size();
            System.out.println(" В массиве inputList " + size0 + " строк");

        } catch (FileNotFoundException e) {
            e.printStackTrace(System.err);
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }

        Collections.sort(inputList, new MyListComparator());
        int size0 = inputList.size();

        long beforGrupping = System.nanoTime();
        System.out.print("Подготовились к группировке за = ");
        System.out.println((beforGrupping - start) / 1_000_000 + " ms");

//Для последующей группировки каждый столбец собираем в Мапу(тел, счетчик)
//Эти мапы (их будет inputList.size(), т.е. пока 11 шт) собираем в лист

        HashMap<Long, List<Integer>>[] groupSearchMap = new HashMap[maxL];
        for (int i = 0; i < maxL; i++) {
            Long t = inputList.get(0).get(i);
            groupSearchMap[i] = new HashMap<>(Map.of(t, new ArrayList<>()));
            //проинициализировали список пока пустыми мапами
        }
        for (int line = 0; line < size0; line++) {
            List<Long> currentList = inputList.get(line);
            for (int i = 0; i < currentList.size(); i++) {
                Long x = currentList.get(i);
                if (x > 0L) {
                    List<Integer> members = new ArrayList<>();
                    if (groupSearchMap[i].containsKey(x)) {
                        members = groupSearchMap[i].get(x);
                    }
                    members.add(line);
                    groupSearchMap[i].put(x, members); //(пере)записываем ключ и строки, в которых он находится
                }
            }
        }//закончили собирать мапы

        long mapIsReady = System.nanoTime();
        System.out.print("Собрали мапы, время = ");
        System.out.println((mapIsReady - start) / 1_000_000 + " ms");
        /*
Если в мапе счетчик(длина листа) >1 ==> это группа
        далее ключи нам не нужны, только value
         пересобираем их в Set отбрасывая одиночные группы
         сравниваем сеты, начиная с конца (там они короче)
        рассортируем список компаратором (по длине сета)
        теперь у нас есть размер группы и её состав - номера строк в исходной таблице
        ==> мы готовы представить результат
         */
        List<Set<Integer>>[] primaryGroups = new ArrayList[maxL];
        int[] numGroup = new int[maxL];
        int totalSum = 0;
        for (int c = 0; c < maxL; c++) {
            numGroup[c] = 0;
            primaryGroups[c] = new ArrayList<>();
            for (HashMap.Entry<Long, List<Integer>> entry : groupSearchMap[c].entrySet()) {
                List<Integer> list = entry.getValue();
                if (list.size() > 1) {
                    Set<Integer> setInt = new HashSet();
                    setInt.addAll(list);
                    primaryGroups[c].add(setInt);
                    numGroup[c]++;
                    totalSum++;
                }
            }
        }
        System.out.println("numGroup = " + Arrays.toString(numGroup));
        System.out.println("totalSum = " + totalSum);
        int count = 0;
        for (int c1 = maxL - 1; c1 >= 1; c1--) {
            for (int c2 = c1 - 1; c2 >= 0; c2--) {
                Set<Integer> setInt = new HashSet();
                for (Set<Integer> setB : primaryGroups[c1]) {
                    for (Set<Integer> setA : primaryGroups[c2]) {
                        if (!setInt.isEmpty()) setInt.clear();
                        setInt.addAll(setA);
                        setInt.addAll(setB);
                        if (setInt.size() < (setA.size() + setB.size())) {
                            count++;
                            setA.addAll(setB);
                            setB.clear();
                        }
                    }
                }
            }
        }

        List<Set<Integer>> resultList = new ArrayList<>();
        for (List<Set<Integer>> set : primaryGroups) {

            resultList.addAll(set.stream().filter(x -> !x.isEmpty()).collect(Collectors.toList()));
        }

        Collections.sort(resultList, new MySetComparator());

        int groupNum = resultList.size();
//
        long resultIsReady = System.nanoTime();
        System.out.print("Готов представить результат, время = ");
        System.out.println((resultIsReady - start) / 1_000_000 + " ms");
        System.out.println("Записываем результат в файл ");

//Итоговый вывод
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(outname));
            writer.write("В итоге получили " + groupNum + " неединичных групп\n");
            writer.write("из них" + count + " имеют больше 2 строк\n");

            int g = 1;
            for (Set<Integer> res : resultList) {
                writer.newLine();
                writer.write("Группа " + (g++));
                for (Integer j : res) {
                    writer.newLine();
                    writer.write(
                            inputList.get(j).stream()
                                    .map(n -> n == 0 ? "" : '"' + String.valueOf(n) + '"')
                                    .collect(Collectors.joining(";")));
                }
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }

        // finish
        long finita = System.nanoTime();
        System.out.print("FINISH, время = ");
        System.out.println((finita - start) / 1_000_000 + " ms");
    }
}

class MyListComparator implements java.util.Comparator<List<Long>> {
    public int compare(List<Long> a, List<Long> b) {
        return b.size() - a.size();
    }
}

class MySetComparator implements java.util.Comparator<Set<Integer>> {
    public int compare(Set<Integer> a, Set<Integer> b) {
        return b.size() - a.size();
    }
}