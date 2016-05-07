package org.aiwolf.ui.bin;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.TreeSet;

import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Player;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.data.Team;
import org.aiwolf.common.net.GameSetting;
import org.aiwolf.common.net.TcpipClient;
import org.aiwolf.common.util.Counter;
import org.aiwolf.server.AIWolfGame;
import org.aiwolf.server.GameData;
import org.aiwolf.server.LostClientException;
import org.aiwolf.server.net.ServerListener;
import org.aiwolf.server.net.TcpipServer;
import org.aiwolf.server.util.FileGameLogger;
import org.aiwolf.server.util.GameLogger;
import org.aiwolf.server.util.MultiGameLogger;
import org.aiwolf.ui.GameViewer;
import org.aiwolf.ui.log.ContestResource;
import org.aiwolf.ui.util.AgentLibraryReader;

import javafx.util.Pair;


/**
 * クライアントを指定して直接シミュレーションを実行する
 * @author tori
 *
 */
public class AutoStarter {

	private Map<String, Pair<String, Role>> roleAgentMap;
	private File libraryDir;
	
	int agentNum = -1;
	int port = 10000;
	int gameNum = 100;
	String logDirName ="./log/";
	private TcpipServer gameServer;
	private GameSetting gameSetting;
	boolean isRunning;
	boolean isSuccessToFinish;
	Thread serverThread;
	boolean isVisualize = false;
	
	boolean initServer=false;
	
	Map<String, Counter<Role>> winCounterMap;
	Map<String, Counter<Role>> roleCounterMap;
	
	
	/**
	 * Start Human Agent Starter
	 * 
	 * @param args
	 * @throws ClassNotFoundException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws InstantiationException, IllegalAccessException, ClassNotFoundException, IOException {
		if(args.length == 0){
			System.err.println("Usage:"+AutoStarter.class.getName()+" initFileName");
			return;
		}
		
		AutoStarter ssbi = new AutoStarter(args[0]);
		ssbi.start();
		ssbi.result();
		System.exit(1);
	}
	
	public AutoStarter(String fileName) throws IOException{

		libraryDir = new File("./");
		roleAgentMap = new HashMap<>();
		File initFile = new File(fileName);
		Path src = initFile.toPath();
		for(String line:Files.readAllLines(src, Charset.forName("UTF8"))){
			if(line.contains("=")){
				String[] data = line.split("=");
				if(data[0].trim().equals("lib")){
					libraryDir = new File(data[1].trim());
				}
				else if(data[0].trim().equals("log")){
					logDirName = data[1].trim();
				}
				else if(data[0].trim().equals("port")){
					port = Integer.parseInt(data[1].trim());
				}
				else if(data[0].trim().equals("agent")){
					agentNum = Integer.parseInt(data[1].trim());
				}
				else if(data[0].trim().equals("game")){
					gameNum = Integer.parseInt(data[1].trim());
				}
				else if(data[0].trim().equals("view")){
					isVisualize = "true".equals(data[1].trim().toLowerCase());
				}
			}
			else{
				String[] data = line.split(",");
				String name = data[0];
				String classPath = data[1];
				Role role = null;
				if(data.length >= 3){
					try{
						role = Role.valueOf(data[2]);
					}catch(IllegalArgumentException e){
					}
				}
				roleAgentMap.put(name, new Pair<String, Role>(classPath, role));
			}
		}
		
		if(agentNum < 5){
			agentNum = roleAgentMap.size();
		}

		Map<String, Class> playerClassMap = getPlayerClassMap(libraryDir);
//		System.out.println(playerClassMap);

		for(String name:roleAgentMap.keySet()){
			String clsName = roleAgentMap.get(name).getKey();
			if(!playerClassMap.containsKey(clsName)){
				throw new IllegalArgumentException("No such agent as "+clsName);
			}
		}
		
	}
	
	public void start() throws SocketTimeoutException, IOException, InstantiationException, IllegalAccessException, ClassNotFoundException{
		startServer();
		startClient();

		while(initServer || isRunning){
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	

	private void startClient() throws InstantiationException, IllegalAccessException, ClassNotFoundException {
		for(String playerName:roleAgentMap.keySet()){
			String clsName = roleAgentMap.get(playerName).getKey();
			Role role = roleAgentMap.get(playerName).getValue();
			
			Player player = (Player)Class.forName(clsName).newInstance();
			//引数にRoleRequestを追加
			TcpipClient client = new TcpipClient("localhost", port, role);
			if(playerName != null){
				client.setName(playerName);
//				System.out.println("Set name "+client.getName());
			}
			if(client.connect(player)){
//				System.out.println("Player connected to server:"+player);
			}

		}
		
	}

	private void startServer() throws SocketTimeoutException, IOException {
		
		gameSetting = GameSetting.getDefaultGame(agentNum);
		
		gameServer = new TcpipServer(port, agentNum, gameSetting);
		gameServer.addServerListener(new ServerListener() {
			
			@Override
			public void unconnected(Socket socket, Agent agent, String name) {
				
			}
			
			@Override
			public void connected(Socket socket, Agent agent, String name) {
				System.out.println("Connected:"+name);
				
			}
		});
//		gameServer.waitForConnection();
//		
//		AIWolfGame game = new AIWolfGame(gameSetting, gameServer);
//		game.setRand(new Random());
//		if(logDirName != null){
//			game.setLogFile(new File(logDirName));
//		}
//		game.start();
		
		Runnable r = new Runnable() {
			
			@Override
			public void run() {
				try{
					gameServer.waitForConnection();
					isRunning = true;
					initServer = false;
					winCounterMap = new HashMap<>();
					roleCounterMap = new HashMap<>();
					for(int i = 0; i < gameNum; i++){
						AIWolfGame game = new AIWolfGame(gameSetting, gameServer);

						game.setRand(new Random(i));
						File logFile = new File(String.format("%s/%03d.log", logDirName, i)); 
						GameLogger logger = new FileGameLogger(logFile);
						if(isVisualize){
							ContestResource resource = new ContestResource(game);
							GameViewer gameViewer = new GameViewer(resource, game);
							gameViewer.setAutoClose(true);
							logger = new MultiGameLogger(logger, gameViewer);
						}
						game.setGameLogger(logger);
						
						try{
							game.start();
	
							Team winner = game.getWinner();
							GameData gameData = game.getGameData();
							for(Agent agent:gameData.getAgentList()){
								String agentName = game.getAgentName(agent);
								if(!winCounterMap.containsKey(agentName)){
									winCounterMap.put(agentName, new Counter<Role>());
								}
								if(!roleCounterMap.containsKey(agentName)){
									roleCounterMap.put(agentName, new Counter<Role>());
								}

								if(winner == gameData.getRole(agent).getTeam()){
									winCounterMap.get(agentName).add(gameData.getRole(agent));
								}
								roleCounterMap.get(agentName).add(gameData.getRole(agent));
							}
						}catch(LostClientException e){
							Agent agent = e.getAgent();
							String teamName = gameServer.getName(agent);
							System.out.println("Lost:"+teamName);
							throw e;
						}
						
					}
					isSuccessToFinish = true;
					gameServer.close();
				}catch(LostClientException e){
					String teamName = gameServer.getName(e.getAgent());
					if(teamName != null){
						System.out.println("Lost connection "+teamName);
					}
				} catch(SocketTimeoutException e){
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				isRunning = false;
			}

		};

		initServer = true;
		serverThread = new Thread(r);
		serverThread.start();

	}

	private void result() {
		for(Role role:Role.values()){
			if(role == Role.FREEMASON){
				continue;
			}
			System.out.print("\t"+role);
		}
		System.out.println("\tTotal");
		for(String name:new TreeSet<>(roleAgentMap.keySet())){
			System.out.print(name+"\t");
			double win = 0;
			double cnt = 0;
			for(Role role:Role.values()){
				if(role == Role.FREEMASON){
					continue;
				}
				System.out.printf("%d/%d\t", winCounterMap.get(name).get(role), roleCounterMap.get(name).get(role));
				win += winCounterMap.get(name).get(role);
				cnt += roleCounterMap.get(name).get(role);
			}
			System.out.printf("%.3f\n", win/cnt);
		}

		
	}


	
	/**
	 * ディレクトリ以下のライブラリから
	 * @param dir
	 * @return
	 * @throws IOException
	 */
	private Map<String, Class> getPlayerClassMap(File dir) throws IOException {
		Map<String, Class> playerClassMap = new HashMap<>();
		for(File file:AgentLibraryReader.getJarFileList(dir)){
			for(Class c:AgentLibraryReader.getPlayerClassList(file)){
				playerClassMap.put(c.getName(), c);
			}
		}
		return playerClassMap;
	}


}