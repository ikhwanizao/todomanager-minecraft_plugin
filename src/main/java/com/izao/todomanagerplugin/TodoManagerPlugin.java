package com.izao.todomanagerplugin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public final class TodoManagerPlugin extends JavaPlugin {

    private Connection conn;

    @Override
    public void onEnable() {
        conn = connect();
        createDatabaseAndTable();
        this.getCommand("todo").setExecutor(new TodoCommandExecutor());
    }

    private Connection connect() {
        String url = "jdbc:sqlite:minecraft.db";
        try {
            return DriverManager.getConnection(url);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void createDatabaseAndTable() {
        String sql = "CREATE TABLE IF NOT EXISTS todo_list (player_id text NOT NULL, player_name text NOT NULL, todo_item text NOT NULL);";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public class TodoCommandExecutor implements CommandExecutor {

        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Only players can use this command!");
                return true;
            }

            Player player = (Player) sender;

            if (args.length == 0) {
                player.sendMessage("Please specify what you want to do!");
                return false;
            }

            String action = args[0];

            if (action.equalsIgnoreCase("add")) {
                addCommand(player, args);
            } else if (action.equalsIgnoreCase("list")) {
                listCommand(player);
            } else if (action.equalsIgnoreCase("remove")) {
                removeCommand(player, args);
            } else {
                player.sendMessage("Invalid action! Use add, list, or remove.");
            }

            return true;
        }

        private void addCommand(Player player, String[] args) {
            String todoItem = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
            addTodoItem(player.getUniqueId().toString(), player.getName(), todoItem);
            player.sendMessage("Added to your todo list: " + todoItem);
        }

        private void listCommand(Player player) {
            List<String> todoList = getTodoList(player.getName());
            if (todoList.isEmpty()) {
                player.sendMessage("Your todo list is empty. Use /todo add <item> to add a new item.");
            } else {
                StringBuilder sb = new StringBuilder("Your todo list:\n");
                for (int i = 0; i < todoList.size(); i++) {
                    sb.append((i + 1) + ". " + todoList.get(i) + "\n");
                }
                player.sendMessage(sb.toString());
            }
        }

        private void removeCommand(Player player, String[] args) {
            if (args.length < 2) {
                player.sendMessage("Please specify the index of the item you want to remove!");
                return;
            }
            int index = Integer.parseInt(args[1]) - 1;
            removeTodoItemByIndex(player, player.getName(), index);
            player.sendMessage("Removed item " + (index + 1) + " from your todo list.");
        }

        private void addTodoItem(String playerId, String playerName, String todoItem) {
            String sql = "INSERT INTO todo_list(player_id, player_name, todo_item) VALUES(?,?,?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, playerId);
                pstmt.setString(2, playerName);
                pstmt.setString(3, todoItem);
                pstmt.executeUpdate();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private List<String> getTodoList(String playerName) {
            String sql = "SELECT todo_item FROM todo_list WHERE player_name = ?";
            List<String> todoList = new ArrayList<>();
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, playerName);
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    todoList.add(rs.getString("todo_item"));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return todoList;
        }

        private void removeTodoItemByIndex(Player player, String playerName, int index) {
            List<String> todoList = getTodoList(playerName);
            if (index < 0 || index >= todoList.size()) {
                player.sendMessage("Invalid index!");
                return;
            }
            String todoItem = todoList.get(index);
            String sql = "DELETE FROM todo_list WHERE player_name = ? AND todo_item = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, playerName);
                pstmt.setString(2, todoItem);
                pstmt.executeUpdate();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}