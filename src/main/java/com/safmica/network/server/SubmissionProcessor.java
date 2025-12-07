package com.safmica.network.server;

import com.safmica.model.Game;
import com.safmica.model.GameAnswer;
import com.safmica.model.Message;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class SubmissionProcessor {
    private final BlockingQueue<GameAnswer> answerQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<GameAnswer> broadcastQueue = new LinkedBlockingQueue<>();
    private final TcpServerHandler server;

    private final ConcurrentHashMap<String, Integer> submitCount = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> cooldownUntil = new ConcurrentHashMap<>();

    private volatile boolean running = true;
    private final Thread evaluatorThread;
    private final Thread broadcasterThread;

    public SubmissionProcessor(TcpServerHandler server) {
        this.server = server;

        evaluatorThread = new Thread(this::evaluatorLoop, "answer-evaluator-thread");
        evaluatorThread.setDaemon(true);

        broadcasterThread = new Thread(this::broadcasterLoop, "answer-broadcaster-thread");
        broadcasterThread.setDaemon(true);

        evaluatorThread.start();
        broadcasterThread.start();
    }

    public void enqueue(GameAnswer answer) {
        if (!running) return;
        answerQueue.offer(answer);
    }

    private void evaluatorLoop() {
        while (running) {
            try {
                GameAnswer answer = answerQueue.take();
                if (answer == null) continue;

                String user = answer.username;

                long now = System.currentTimeMillis();
                Long until = cooldownUntil.getOrDefault(user, 0L);
                if (until > now) {
                    sendReject(answer, "COOLDOWN");
                    continue;
                }

                int used = submitCount.getOrDefault(user, 0);
                if (used >= 3) {
                    sendReject(answer, "LIMIT");
                    continue;
                }

                submitCount.put(user, used + 1);

                boolean correct = evaluateAnswer(answer);
                answer.status = correct;

                broadcastQueue.offer(answer);

                cooldownUntil.put(user, System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(10));

                if (correct) {
                    answerQueue.clear();
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private boolean evaluateAnswer(GameAnswer answer) {
        try {
            Game game = server.getGame();
            if (game == null) return false;

            String expr = sanitizeExpression(answer.answer);

            Expression e = new ExpressionBuilder(expr).build();
            double result = e.evaluate();

            double target = game.getX();
            return Math.abs(result - target) < 0.0001;
        } catch (Exception ex) {
            return false;
        }
    }

    private String sanitizeExpression(String input) {
        if (input == null) return "0";
        String s = input.replaceAll("×", "*")
                .replaceAll("÷", "/")
                .replaceAll("²", "^2");

        s = s.replaceAll("√\\(", "(");
        while (s.contains("(")) { 
            break;
        }

        s = s.replaceAll("\\s+", "");
        return s;
    }

    private void sendReject(GameAnswer answer, String reason) {
        try {
            Message<String> msg = new Message<>("SUBMIT_REJECT", reason);
            // send only to the submitting client
            server.sendToClient(answer.username, msg);
        } catch (Exception ignored) {
        }
    }

    private void broadcasterLoop() {
        while (running) {
            try {
                GameAnswer toBroadcast = broadcastQueue.take();
                if (toBroadcast == null) continue;

                Message<GameAnswer> m = new Message<>("GAME_RESULT", toBroadcast);
                server.broadcastMessage(m);

                if (!toBroadcast.status) {
                    Thread.sleep(TimeUnit.SECONDS.toMillis(5));
                } else {
                    Thread.sleep(TimeUnit.SECONDS.toMillis(10));
                    submitCount.clear();
                    cooldownUntil.clear();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public void shutdown() {
        running = false;
        evaluatorThread.interrupt();
        broadcasterThread.interrupt();
    }
}
