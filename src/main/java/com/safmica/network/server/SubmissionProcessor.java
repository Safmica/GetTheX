package com.safmica.network.server;

import com.safmica.model.Game;
import com.safmica.model.GameAnswer;
import com.safmica.model.Message;
import com.safmica.model.PlayerLeaderboard;
import com.safmica.model.Room;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.stream.Collectors;

public class SubmissionProcessor {
    private final BlockingQueue<GameAnswer> answerQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<GameAnswer> broadcastQueue = new LinkedBlockingQueue<>();
    private final TcpServerHandler server;
    private Game game;
    private Room room;

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
        if (!running)
            return;
        answerQueue.offer(answer);
    }

    private void evaluatorLoop() {
        while (running) {
            try {
                GameAnswer answer = answerQueue.take();
                if (answer == null)
                    continue;
                if (answer.round != game.getRound()) {
                    sendAck(answer, "ROUND_OVER");
                    continue;
                }
                String user = answer.username;

                long now = System.currentTimeMillis();
                Long until = cooldownUntil.getOrDefault(user, 0L);
                if (until > now) {
                    sendAck(answer, "COOLDOWN");
                    continue;
                }

                int used = submitCount.getOrDefault(user, 0);
                if (used >= 3) {
                    sendAck(answer, "LIMIT");
                    continue;
                }

                if (server.isFinalRoundActive() && !server.isFinalRoundPlayer(user)) {
                    sendAck(answer, "NOT_ALLOWED_FINAL");
                    continue;
                }

                sendAck(answer, "RECEIVED");
                submitCount.put(user, used + 1);

                answer = evaluateAnswer(answer);

                broadcastQueue.offer(answer);

                cooldownUntil.put(user, System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(10));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private GameAnswer evaluateAnswer(GameAnswer answer) {
        try {
            Game game = server.getGame();
            if (game == null) {
                answer.status = false;
                return answer;
            }

            String expr = sanitizeExpression(answer.answer);

            Expression e = new ExpressionBuilder(expr).build();
            double result = e.evaluate();

            double target = game.getX();
            answer.x = result;
            if (Math.abs(result - target) < 0.0001) {
                answer.status = true;
            } else {
                answer.status = false;
            }
            return answer;
        } catch (Exception ex) {
            answer.status = false;
            return answer;
        }
    }

    private String sanitizeExpression(String input) {
        if (input == null)
            return "0";
        String s = input;

        s = s.replaceAll("×", "*")
                .replaceAll("÷", "/")
                .replaceAll("²", "^2")
                .replaceAll("\\?", "*");

        s = s.replaceAll("√\\s*\\(", "sqrt(");

        s = s.replaceAll("√\\s*(\\d+(?:\\.\\d+)?)", "sqrt($1)");

        s = s.replaceAll("\\)\\s*\\(", ")*(");
        s = s.replaceAll("(?<=[0-9\\.])\\s*(?=\\()", "*");
        s = s.replaceAll("\\)\\s*(?=\\d)", ")*");
        s = s.replaceAll("(?<=[0-9\\)])(?=√)", "*");

        s = s.replaceAll("\\s+", "");
        return s;
    }

    private void sendAck(GameAnswer answer, String reason) {
        try {
            Message<String> msg = new Message<>("SUBMIT_ACK", reason);
            server.sendToClient(answer.username, msg);
        } catch (Exception ignored) {
        }
    }

    private void broadcasterLoop() {
        while (running) {
            try {
                GameAnswer answer = broadcastQueue.take();
                if (answer == null)
                    continue;

                Message<GameAnswer> m = new Message<>("GAME_RESULT", answer);
                server.broadcastMessage(m);

                if (!answer.status) {
                    Thread.sleep(TimeUnit.SECONDS.toMillis(5));
                } else {
                    server.addPointToPlayer(answer.username);

                    int currentRoundBefore = game.getRound();
                    int totalRound = room == null ? 0 : room.getTotalRound();

                    if (totalRound > 0 && currentRoundBefore >= totalRound) {
                        List<PlayerLeaderboard> lb = game.getLeaderboard();
                        if (lb == null || lb.isEmpty()) {
                            Thread.sleep(TimeUnit.SECONDS.toMillis(3));
                            server.roundOver(answer.username);
                        } else {
                            int max = lb.stream().mapToInt(PlayerLeaderboard::getScore).max().orElse(0);
                            List<PlayerLeaderboard> top = lb.stream().filter(p -> p.getScore() == max).collect(Collectors.toList());
                            if (top.size() == 1) {
                                Thread.sleep(TimeUnit.SECONDS.toMillis(3));
                                server.roundOver(top.get(0).getName());
                                if (server.isFinalRoundActive()) server.endFinalRound();
                            } else {
                                List<String> finalists = top.stream().map(PlayerLeaderboard::getName).collect(Collectors.toList());
                                Thread.sleep(TimeUnit.SECONDS.toMillis(10));
                                game.nextRound();
                                server.startFinalRound(finalists);
                            }
                        }
                    } else {
                        game.nextRound();
                        answerQueue.clear();
                        submitCount.clear();
                        cooldownUntil.clear();

                        Thread.sleep(TimeUnit.SECONDS.toMillis(10));

                        server.nextRound();
                    }
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

    public synchronized PlayerLeaderboard findByUsername(String username) {
        for (PlayerLeaderboard p : game.getLeaderboard()) {
            if (username.equals(p.getName()))
                return p;
        }
        return null;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public void setGame(Game game) {
        this.game = game;
    }
}
