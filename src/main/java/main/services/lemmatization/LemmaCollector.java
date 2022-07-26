package main.services.lemmatization;

import main.model.LemmaValues;
import main.model.Word;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

public class LemmaCollector {

    private final Map<String, LemmaValues> lemmasMap = new HashMap<>();

    ExecutorService service = Executors.newFixedThreadPool(2);
    List<FutureTask<Word>> tasks = new ArrayList<>();
    private int textLength;

    public Map<String, LemmaValues> getLemmas(String text) {
        List<Word> words = new ArrayList<>();
        getWordsFromText(text).stream()
                .map(this::wordFormatterToLowerCase)
                .filter(this::wordValidator)
                .forEach(w -> {
                    FutureTask<Word> futureTask = new FutureTask<>(new Lemmatizer(w));
                    tasks.add(futureTask);
                });
        tasks.forEach(t -> service.submit(t));
        tasks.forEach(t -> {
            try {
                words.add(t.get());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        words.forEach(w -> w.getLemmas()
                .forEach(l -> {
                    LemmaValues values = lemmasMap.getOrDefault(l, new LemmaValues(l, 0, new ArrayList<>(), textLength));
                    List<Integer> wordNumbers = values.getWordNumbers();
                    wordNumbers.add(w.getNumber());
                    values.setCount(values.getCount() + 1);
                    values.setWordNumbers(wordNumbers);
                    lemmasMap.put(l, values);
                }));
        service.shutdown();
        return lemmasMap;
    }

    private List<Word> getWordsFromText(String text) {
        List<Word> wordsList = new ArrayList<>();
        List<String> words = Arrays.stream(text.split("\\s+")).toList();
        textLength = words.size();
        for (int i = 0; i < words.size(); i++) {
            Word word = new Word(words.get(i));
            word.setNumber(i);
            wordsList.add(word);
        }
        return wordsList;
    }

    public Word wordFormatterToLowerCase(Word word) {
        String content = word.getWord().replaceAll("[.,!?'\"]+", "").toLowerCase();
        word.setWord(content);
        return word;
    }

    public boolean wordValidator(Word word) {
        return word.getWord().matches("[а-яё]+");
    }
}
