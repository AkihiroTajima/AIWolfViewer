package org.aiwolf.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.border.LineBorder;

import org.aiwolf.client.lib.Content;
import org.aiwolf.client.lib.TalkType;
import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.data.Status;
import org.aiwolf.common.data.Talk;
import org.aiwolf.common.data.Team;
import org.aiwolf.common.data.Vote;
import org.aiwolf.common.net.GameInfo;
import org.aiwolf.common.net.GameSetting;
import org.aiwolf.ui.res.AIWolfResource;

public class InformationPanel extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	final public static Color PLAYER_COLOR = HumanPlayer.PLAYER_COLOR;
	final public static Color TALK_COLOR = HumanPlayer.TALK_COLOR;
	final public static Color ACTION_COLOR = HumanPlayer.ACTION_COLOR;
	final public static Color WHISPER_COLOR = HumanPlayer.WHISPER_COLOR;
	final public static Color FRIEND_COLOR = HumanPlayer.FRIEND_COLOR;
	final public static Color FOCUS_COLOR = ACTION_COLOR;
	
//	protected JPanel titlePanel;
	protected EventPanel eventPanel;
	protected TalkPanel talkPanel;
//	private JPanel informationPanel;
	
	private Map<Agent, AgentPanel> agentPanelMap;
	
	private GameSetting gameSetting;
//	private JLabel remainLabel;
	
	private AIWolfResource resource;
	
	GameInfo gameInfo;

	WaitListener waitListener;
	
	static final Comparator<Vote> voteComparator = new Comparator<Vote>() {
		@Override
		public int compare(Vote o1, Vote o2) {
			if(o1.getTarget() == o2.getTarget()){
				return o1.getAgent().compareTo(o2.getAgent());
			}
			else{
				return o1.getTarget().compareTo(o2.getTarget());
			}
		}
	};


	
	public InformationPanel(AIWolfResource resource) {
		this.resource = resource;

		setLayout(new BorderLayout());

		eventPanel = new EventPanel();
		talkPanel = new TalkPanel(resource);

		add(talkPanel, BorderLayout.CENTER);
		add(eventPanel, BorderLayout.NORTH);

//		JPanel mainPanel = new JPanel(new BorderLayout());
//		mainPanel.setOpaque(false);
//		mainPanel.add(talkPanel, BorderLayout.CENTER);
//		mainPanel.add(eventPanel, BorderLayout.NORTH);
//
//		add(mainPanel, BorderLayout.CENTER);

	}

	public void initialize(GameInfo gameInfo, GameSetting gameSetting){
		this.gameSetting = gameSetting;
		this.gameInfo = gameInfo;

//		FlowLayout flowLayout = new FlowLayout();
//		flowLayout.setAlignment(FlowLayout.LEFT);
//		flowLayout.setAlignOnBaseline(true);
//		agentListPanel.setLayout(flowLayout);
		eventPanel.setBorder(new LineBorder(Color.BLACK));
		agentPanelMap = new HashMap<>();
		List<AgentPanel> panelList = new ArrayList<AgentPanel>();
		for(Agent agent:new TreeSet<Agent>(gameInfo.getAgentList())){
//			AgentPanel panel = new AgentPanel(agent, gameInfo.getRoleMap().get(agent), agent.equals(gameInfo.getAgent()), talkConverter);
		
			boolean isPlayer = agent.equals(gameInfo.getAgent());
			AgentPanel agentPanel = new AgentImagePanel(agent, gameInfo.getRoleMap().get(agent), isPlayer, resource);
			panelList.add(agentPanel);
			
			eventPanel.add(agentPanel);
			agentPanelMap.put(agent, agentPanel);
		}
		
		((EventPanel)eventPanel).setAgentPanelList(panelList);

		talkPanel.initialize(gameInfo, gameSetting);
//		inform(String.format("%s\n",resource.getFirstText(gameInfo.getAgent(), gameInfo.getRole())), HumanPlayer.PLAYER_COLOR);


	}
	
	public void firstInformation(GameInfo gameInfo, GameSetting gameSetting){
		inform(resource.getRoleInformation(gameSetting.getRoleNumMap()), PLAYER_COLOR);
		
	}
	
	/**
	 * 
	 * @param gameInfo
	 */
	public void update(GameInfo gameInfo) {
		this.gameInfo = gameInfo;
//		dayLabel.setText("Day "+gameInfo.getDay()+"");
//		remainLabel.setText(gameInfo.getAliveAgentList().size()+"");
		
		for(Agent agent:gameInfo.getAgentList()){
			agentPanelMap.get(agent).setStatus(gameInfo.getStatusMap().get(agent));
			agentPanelMap.get(agent).setRole(gameInfo.getRoleMap().get(agent));
		}

		for (Agent agent : gameInfo.getLastDeadAgentList()) {
			if (agent != gameInfo.getExecutedAgent()) {
				if(agent == gameInfo.getAttackedAgent()){
					agentPanelMap.get(agent).setAttacked(gameInfo.getDay()); 
				}
				else{
					agentPanelMap.get(agent).setDead(gameInfo.getDay()); 
				}
			}
		}
		if (gameInfo.getExecutedAgent() != null) {
			agentPanelMap.get(gameInfo.getExecutedAgent()).setExecuted(gameInfo.getDay());
		}
	
		talkPanel.update(gameInfo);
		talkPanel.scrollToTail();
	}

	/**
	 * 
	 * @param gameInfo
	 */
	public void finish(GameInfo gameInfo) {

	}

	/**
	 * 
	 * @param agent
	 * @param role
	 */
	public void setComingOut(Agent agent, Role role) {
		agentPanelMap.get(agent).setComingOut(role);
	}
	
	/**
	 * 
	 * @param voteList
	 * @return
	 */
	public boolean updateLatestVote(List<Vote> voteList) {
		if(voteList.isEmpty()){
			return false;
		}
		JTextArea textArea = talkPanel.createTextPanel(gameInfo.getDay(), resource.convertVote(), ACTION_COLOR);
		talkPanel.addItem(gameInfo.getDay(), textArea);

		updateVote(voteList, false);
		
		return true;
		
	}
	
	/**
	 * @param day
	 * @param talkList
	 * @return
	 * @see org.aiwolf.ui.TalkPanel#updateTalk(int, java.util.List)
	 */
	public boolean updateTalk(int day, List<Talk> talkList) {
		return talkPanel.updateTalk(day, talkList);
	}
	
	/**
	 * 
	 * @param day
	 * @param talk
	 * @param talkType
	 * @return
	 */
	public boolean updateTalk(int day, Talk talk, TalkType talkType) {
		if(!talk.getText().equals(Talk.OVER) && !talk.getText().equals(Talk.SKIP)){
			talkPanel.addTalk(day, talk, talkType);
			if(talkType == TalkType.TALK){
				talkPanel.setLastTalkIdx(talk.getIdx()+1);
			}
			else{
				talkPanel.setLastWhisperIdx(talk.getIdx()+1);
			}
			JPanel tp = talkPanel.createTalkPanel(talk, talkType);
			eventPanel.addCenterItem(tp);

			agentPanelMap.get(talk.getAgent()).setBackground(FOCUS_COLOR);
			
			try{
				Content u = new Content(talk.getText());
				
				eventPanel.clearArrow();
				if(talkType == TalkType.TALK && u.getTopic() != null){
					switch(u.getTopic()){
					case ATTACK:
						eventPanel.addArrow(talk.getAgent(), u.getTarget(), Color.RED);
						break;
					case DIVINED:
					case IDENTIFIED:
						eventPanel.addArrow(talk.getAgent(), u.getTarget(), new Color(128,64,255));
						break;
					case GUARDED:
					case VOTE:
					case ESTIMATE:
						eventPanel.addArrow(talk.getAgent(), u.getTarget(), Color.BLUE);
						break;
					default:
						break;
					}
				}
			}catch(NullPointerException |ArrayIndexOutOfBoundsException e){
				
			}
			return true;
		}
		return false;
	}
	
//	public boolean updateWhisper(int day, Talk talk) {
//		if(!talk.getContent().equals(Talk.OVER) && !talk.getContent().equals(Talk.SKIP)){
//			talkPanel.addTalk(day, talk, TalkType.WHISPER);
//			talkPanel.lastWhisperIdx = talk.getIdx();
//			return true;
//		}
//		return false;
//	}

	

	/**
	 * @return
	 * @see org.aiwolf.ui.TalkPanel#getLastTalkIdx()
	 */
	public int getLastTalkIdx() {
		return talkPanel.getLastTalkIdx();
	}

	/**
	 * @return
	 * @see org.aiwolf.ui.TalkPanel#getLastWhisperIdx()
	 */
	public int getLastWhisperIdx() {
		return talkPanel.getLastWhisperIdx();
	}

	/**
	 * @param day
	 * @param whisperList
	 * @return
	 * @see org.aiwolf.ui.TalkPanel#updateWhisper(int, java.util.List)
	 */
	public boolean updateWhisper(int day, List<Talk> whisperList) {
		return talkPanel.updateWhisper(day, whisperList);
	}
	

	/**
	 * 
	 */
	public void scrollToTail(){
		talkPanel.scrollToTail();
	}

	/**
	 * @param gameInfo
	 * @see org.aiwolf.ui.TalkPanel#dayStart(org.aiwolf.common.net.GameInfo)
	 */
	public void dayStart(GameInfo gameInfo) {
//		talkPanel.dayStart(gameInfo);
		
		this.gameInfo = gameInfo;
		int day = gameInfo.getDay();

		eventPanel.clearArrow();
		inform(resource.dayStart(gameInfo.getDay()), ACTION_COLOR);
//		talkPanel.addText(day, resource.dayStart(gameInfo.getDay()));

		///////////////////////////////////////////////////////
		//Vote
		TreeSet<Vote> voteSet = new TreeSet<>(voteComparator);
		voteSet.addAll(gameInfo.getVoteList());
		updateVote(voteSet, true);
		
		///////////////////////////////////////////////////////
		//Divine Medium
		if(gameInfo.getDivineResult() != null){
			Agent seer = gameInfo.getDivineResult().getAgent();
			Agent target = gameInfo.getDivineResult().getTarget();
			
			if(gameInfo.getStatusMap().get(seer) == Status.ALIVE){
				inform(resource.convertDivined(gameInfo.getDivineResult()), ACTION_COLOR, seer, target);
			}
		}
		if(gameInfo.getMediumResult() != null){
			Agent agent = gameInfo.getMediumResult().getAgent();
			Agent target = gameInfo.getMediumResult().getTarget();
			
			if(gameInfo.getStatusMap().get(agent) == Status.ALIVE){
				inform(resource.convertMedium(gameInfo.getMediumResult()), ACTION_COLOR, agent, target);
			}
//			talkPanel.addAgentInformation(day, target, resource.convertMedium(gameInfo.getMediumResult()));
		}
		
		///////////////////////////////////////////////////////
		//Attack
		TreeSet<Vote> attackVoteSet = new TreeSet<>(voteComparator);
		attackVoteSet.addAll(gameInfo.getAttackVoteList());
		if(!attackVoteSet.isEmpty()){
			eventPanel.clearCenterPanel();
			eventPanel.clearArrow();
			for(Vote attackVote:attackVoteSet){
				if(gameInfo.getStatusMap().get(attackVote.getAgent()) == Status.ALIVE){
					eventPanel.addArrow(attackVote.getAgent(), attackVote.getTarget(), Color.RED);
					talkPanel.addText(day, resource.convertAttackVote(attackVote), WHISPER_COLOR);
				}
			}
			if(waitListener != null){
				waitListener.waitForNext();
			}
		}
		///////////////////////////////////////////////////////
		//Guard
		if(gameInfo.getGuardedAgent() != null){
			Agent guard = null;
			for(Agent agent:gameInfo.getAliveAgentList()){
				if(gameInfo.getRoleMap().get(agent) == Role.BODYGUARD){
					guard = agent;
					break;
				}
			}
			if(gameInfo.getStatusMap().get(guard) == Status.ALIVE){
				inform(resource.convertGuarded(gameInfo.getGuardedAgent()), ACTION_COLOR, guard, gameInfo.getGuardedAgent());
			}
//			talkPanel.addAgentInformation(day, gameInfo.getGuardedAgent(), resource.convertGuarded(gameInfo.getGuardedAgent()));
		}

		if(gameInfo.getAttackedAgent() != null){
			inform(resource.convertAttacked(gameInfo.getAttackedAgent()), ACTION_COLOR, gameInfo.getAttackedAgent()); // TODO 妖狐考慮
		}

		Agent deadAgent = null;
		for (Agent agent : gameInfo.getLastDeadAgentList()) {
			if (agent != gameInfo.getExecutedAgent()) {
				if(agent == gameInfo.getAttackedAgent()){
					inform(resource.convertDead(agent), WHISPER_COLOR, agent); // TODO 妖狐考慮
				}
				else{
					inform(resource.convertDead(agent), WHISPER_COLOR, agent); // TODO 妖狐考慮
				}
				deadAgent = agent;
			}
		}

		if (deadAgent == null && gameInfo.getDay() > 1) {
			inform(resource.convertDead(null), PLAYER_COLOR);
		}
//		addText(day, attackText.toString(), HumanPlayer.FRIEND_COLOR);
		

		talkPanel.addText(day, resource.aliveRemain(gameInfo.getAliveAgentList().size()));
		clearArrow();
	}

	/**
	 * Show Vote Result
	 * @param voteSet
	 * @param isFinalVote
	 */
	public void updateVote(Collection<Vote> voteSet, boolean isFinalVote) {
		int day = gameInfo.getDay();
		if(!voteSet.isEmpty()){
			eventPanel.clearCenterPanel();
			for(Vote vote:voteSet){
				eventPanel.addArrow(vote.getAgent(), vote.getTarget(), Color.RED);
				Color color = HumanPlayer.TALK_COLOR;
				if(vote.getAgent() == gameInfo.getAgent()){
					color = PLAYER_COLOR;
				}
				else if(gameInfo.getRoleMap().get(vote.getAgent()) == gameInfo.getRole()){
					color = FRIEND_COLOR;
				}
				
				talkPanel.addText(day, resource.convertVote(vote), color);
			}
			if(waitListener != null){
				waitListener.waitForNext();
			}
		}
		if (isFinalVote && gameInfo.getExecutedAgent() != null) {
			informExecutedAgent(gameInfo.getExecutedAgent());
		}
		eventPanel.clearArrow();
	}

	/**
	 * 
	 * @param agent
	 */
	public void informExecutedAgent(Agent agent) {
		inform(resource.convertExecuted(agent), ACTION_COLOR, agent);
	}



	protected void inform(String text,Color color) {
		inform(text, color, null, null);
	}

	protected void inform(String text,Color color, Agent agent) {
		inform(text, color, agent, null);
	}

	/**
	 * 
	 * @param agent
	 * @param text
	 * @param color
	 */
	protected void inform(String text,Color color, Agent agent, Agent target) {
		int day = gameInfo.getDay();
		if(agent != null){
			JPanel panel = talkPanel.createLogPanel(day, agent, text, color);
			talkPanel.addItem(day, panel);
			JPanel panel2 = talkPanel.createLogPanel(day, agent, text, color);
			eventPanel.addCenterItem(panel2);
		}
		else{
			JTextArea textArea = talkPanel.createTextPanel(day, text, color);
			talkPanel.addItem(day, textArea);
			JTextArea textArea2 = talkPanel.createTextPanel(day, text, color);
//			eventPanel.addCenterItem(textArea2);
			JPanel centerPanel = new JPanel(new BorderLayout());
			centerPanel.add(textArea2, BorderLayout.CENTER);
			eventPanel.addCenterItem(centerPanel);
		}

		if(agent != null && target != null){
			eventPanel.addArrow(agent, target, Color.GREEN);
		}
		else{
//			eventPanel.clearArrow();
		}
		if(waitListener != null){
			waitListener.waitForNext();
		}
		
	}

	public void setWinner(int day, Team winner) {
		
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		Color color = PLAYER_COLOR;
		if(gameInfo.getRole() != null && winner != gameInfo.getRole().getTeam()){
			if(winner == Team.WEREWOLF){
				color = HumanPlayer.WHISPER_COLOR;
			}
			else{
				color = HumanPlayer.TALK_COLOR;
			}
		}
		if(gameInfo != null && gameInfo.getRole() != null){
			inform(resource.convertWinner(winner, gameInfo.getRole().getTeam()), color);
		}
		else{
			inform(resource.convertWinner(winner), color);
		}
		scrollToTail();
//		dailyTalkPane.setSelectedIndex(dailyTalkPane.getComponentCount()-1);
//		
//		talkPanel.setWinner(day, winner);
		
	}

	/**
	 * @return waitListener
	 */
	public WaitListener getWaitListener() {
		return waitListener;
	}

	/**
	 * @param waitListener セットする waitListener
	 */
	public void setWaitListener(WaitListener waitListener) {
		this.waitListener = waitListener;
	}

	public void clearArrow() {
		eventPanel.clearArrow();
		for(AgentPanel ap:agentPanelMap.values()){
			ap.setStatus(gameInfo.getStatusMap().get(ap.getAgent()));
		}
	}

	/**
	 * @return resource
	 */
	public AIWolfResource getResource() {
		return resource;
	}

	/**
	 * @param resource セットする resource
	 */
	public void setResource(AIWolfResource resource) {
		this.resource = resource;
		talkPanel.setResource(resource);
	}

	

}
