package org.example;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

public class Main {
    public static void main(String[] args) {

        long start = System.nanoTime();
        String outname;
        try {
            outname = args[0];
        } catch (ArrayIndexOutOfBoundsException e) {
            outname = "lng-4-group.txt";
            System.out.println("Результат запишем в файл по-умолчанию: lng-4-group.txt");
        }
// outname - имя файла для записи результатов
        List<List<Long>> inputList = new ArrayList<>(); //быстрый способ создать коллекцию
        int maxL = 0; //максимальная длина строки (пока для справки)
        try (GZIPInputStream gzip = new GZIPInputStream(new URL("https://github.com/PeacockTeam/new-job/releases/download/v1.0/lng-4.txt.gz").openStream());
             BufferedReader br = new BufferedReader(new InputStreamReader(gzip))) {
            String line = null;
            while ((line = br.readLine()) != null) {
                List<String> scanList = Arrays.asList(line.split(";"));
// разделили строку на слова по разделителю ";"
                try {
// хотим лист слов преобразовать в лист чисел
                    List<Long> result = new ArrayList<>();
                    for (String s : scanList) {
                        long sumVal = 0;
                        String x = s.replaceAll("[^0-9]", "");
                        Long val = (x.length() > 2 ? Long.valueOf(x) : 0L);
                        result.add(val);
                    }

                    if (result.size() > maxL) maxL = result.size();
                    long sumPhones = result.stream().mapToLong(i -> i).sum();
                    if (sumPhones > 0) {
                        result.add(0, sumPhones); // типа HeshCode строки
                        inputList.add(result);
                    }
// т.е. в каждой строке у нас записан сначала Хеш = сумме элементов,
// а далее сами элементы - "0" либо 11-значные числа
                } catch (NumberFormatException e) {
                    System.out.println("Ошибка NumberFormat: " + e.getMessage());
                    // "бракованные" строки пропускаем
                }
            }

        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
        inputList.sort(new MyListComparator());
        int inputListSize = inputList.size();
// "Search of twins"
        List<Integer> twinsList = new ArrayList<>();
        for (int i = 0; i < inputListSize - 1; i++) {
            long hash1 = inputList.get(i).get(0);
            long hash2 = inputList.get(i + 1).get(0);
            if (hash1 == hash2) {
//Хеши равны, теперь проверяем все элементы попарно
                int listLen = inputList.get(i).size();
                if (listLen != inputList.get(i + 1).size()) break;
                int originNums = 0;
                for (int c = 1; c < listLen; c++) {
                    if (inputList.get(i).get(c) != inputList.get(i + 1).get(c)) originNums++;
                }
                if (originNums == 0) {// полное совпадение по всем полям
                    twinsList.add(i + 1);
                }
            }
        }
// собрали в лист номера строк-дубликатов
        if (twinsList.size() > 0) {
            twinsList.stream().sorted(Comparator.reverseOrder());//отсортировали по убыванию
            for (int d : twinsList) {
                inputList.remove(d); // Удалили из списка строки-дубликаты (сначала с бОльшими номерами)
            }
            inputListSize = inputList.size(); //обновили значение с учетом удалённых строк
        }

//        long beforGrupping = System.nanoTime();
//        System.out.print("Подготовились к группировке за = ");
//        System.out.println((beforGrupping - start) / 1_000_000 + " ms");

//Для последующей группировки каждый столбец собираем в Мапу(тел, счетчик)
//Эти мапы (их будет inputList.size(), т.е. пока 11 шт) собираем в лист

        HashMap<Long, List<Integer>>[] groupSearchMap = new HashMap[maxL];
        for (int i = 0; i < maxL; i++) {
            Long t = inputList.get(0).get(i + 1);//(т.к. в 0-й колонке наш "ХэшКод")
            groupSearchMap[i] = new HashMap<>(Map.of(t, new ArrayList<>()));
            //проинициализировали список пока пустыми мапами
        }
        for (int line = 0; line < inputListSize; line++) {
            List<Long> currentList = inputList.get(line);
            for (int j = 0; j < currentList.size() - 1; j++) {
                int i = j + 1;
                Long x = currentList.get(i);
                if (x > 0L) {
                    List<Integer> members = new ArrayList<>();
                    if (groupSearchMap[j].containsKey(x)) {
                        members = groupSearchMap[j].get(x);
                    }
                    members.add(line);
                    groupSearchMap[i - 1].put(x, members); //(пере)записываем ключ и строки, в которых он находится
                }
            }
        }//закончили собирать мапы

//        long mapIsReady = System.nanoTime();
//        System.out.print("Собрали мапы, время = ");
//        System.out.println((mapIsReady - start) / 1_000_000 + " ms");
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
                if (list.size() > 1) {// ==> группа
                    Set<Integer> setInt = new HashSet<>(list);
                    primaryGroups[c].add(setInt);
                    numGroup[c]++;
                    totalSum++;
                }
            }
        }
        System.out.println("Первичная группировка ");
        System.out.println("Частотное распределение совпадений по колонкам");
        System.out.println("numGroup = " + Arrays.toString(numGroup));
        System.out.println("Всего совпадений = " + totalSum);
        int count = 0;
        for (int c1 = maxL - 1; c1 >= 1; c1--) {
            for (int c2 = c1 - 1; c2 >= 0; c2--) {
                Set<Integer> setInt = new HashSet<>();
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
        System.out.println("Финальная группировка ");

        List<Set<Integer>> resultList = new ArrayList<>();
        for (List<Set<Integer>> set : primaryGroups) {
            resultList.addAll(set.stream().filter(x -> !x.isEmpty()).toList());
        }
        resultList.sort(new MySetComparator());

        int groupNum = resultList.size();
//
//        long resultIsReady = System.nanoTime();
//        System.out.print("Готов представить результат, время = ");
//        System.out.println((resultIsReady - start) / 1_000_000 + " ms");
//        System.out.println("Записываем результат в файл ");

//Итоговый вывод
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(outname, StandardCharsets.UTF_8));
            writer.write("В итоге получили " + groupNum + " неединичных групп\n");
            writer.write("из них " + count + " имеют больше 2 строк\n");
//            List<Long> outputList = new ArrayList<>();
            int g = 1;
            for (Set<Integer> res : resultList) {
                writer.newLine();
                writer.write("Группа " + (g++));
                for (Integer j : res) {
                    List<Long> outputList = inputList.get(j)
                            .stream().skip(1).toList();
                    writer.newLine();
                    writer.write(
                            outputList.stream()
                                    .map(n -> n == 0 ? "\"\"" : '"' + String.valueOf(n) + '"')
                                    .collect(Collectors.joining(";")));
                }
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }

        // finish
        System.out.println("В итоге получили " + groupNum + " неединичных групп");
        System.out.println("из них " + count + " имеют больше 2 строк");
        long finita = System.nanoTime();
        System.out.print("FINISH, время = ");
        System.out.println((finita - start) / 1_000_000 + " ms");

    }
}

class MyListComparator implements java.util.Comparator<List<Long>> {
    public int compare(List<Long> a, List<Long> b) {
        int res = b.size() - a.size();
        if (res != 0) return res;
        long l = b.get(0) - a.get(0);
        if (l == 0) return 0;
        else return (l > 0 ? 1 : -1);
    }
}

class MySetComparator implements java.util.Comparator<Set<Integer>> {
    public int compare(Set<Integer> a, Set<Integer> b) {
        return b.size() - a.size();
    }
}
