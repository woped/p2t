package de.dhbw.woped.process2text.service.text.planning;

import de.dhbw.woped.process2text.model.dsynt.DSynTConditionSentence;
import de.dhbw.woped.process2text.model.dsynt.DSynTMainSentence;
import de.dhbw.woped.process2text.model.dsynt.DSynTSentence;
import de.dhbw.woped.process2text.model.intermediate.AbstractFragment;
import de.dhbw.woped.process2text.model.intermediate.ConditionFragment;
import de.dhbw.woped.process2text.model.intermediate.ExecutableFragment;
import de.dhbw.woped.process2text.model.process.ProcessModel;
import de.dhbw.woped.process2text.service.content.determination.extraction.GatewayExtractor;
import de.dhbw.woped.process2text.service.content.determination.label_analysis.EnglishLabelHelper;
import de.dhbw.woped.process2text.service.text.planning.record.ConverterRecord;
import de.dhbw.woped.process2text.service.text.planning.record.ModifierRecord;
import de.hpi.bpt.graph.algo.rpst.RPST;
import de.hpi.bpt.graph.algo.rpst.RPSTNode;
import de.hpi.bpt.process.ControlFlow;
import de.hpi.bpt.process.Node;
import java.util.ArrayList;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class TextToIntermediateConverter {

  Logger logger = LoggerFactory.getLogger(TextToIntermediateConverter.class);
  private final RPST<ControlFlow, Node> rpst;
  private final EnglishLabelHelper lHelper;
  private final boolean imperative;
  private final String imperativeRole;
  private final ProcessModel process;

  TextToIntermediateConverter(
      RPST<ControlFlow, Node> rpst,
      ProcessModel p,
      EnglishLabelHelper lHelper,
      String imperativeRole,
      boolean imperative) {
    this.rpst = rpst;
    this.process = p;
    this.lHelper = lHelper;
    this.imperative = imperative;
    this.imperativeRole = imperativeRole;
  }

  // *********************************************************************************************
  //										OR - SPLIT
  // *********************************************************************************************

  // The following optional parallel paths are available.
  ConverterRecord convertORSimple(RPSTNode<ControlFlow, Node> node) {
    // Create sentence "The following optional paths are available."
    ExecutableFragment eFrag = new ExecutableFragment("execute", "paths", "", "");
    ModifierRecord modRecord2 =
        new ModifierRecord(ModifierRecord.TYPE_ADJ, ModifierRecord.TARGET_BO);
    modRecord2.addAttribute("adv-type", "sentential");
    eFrag.addMod("one or more of the", modRecord2);
    ModifierRecord modRecord3 =
        new ModifierRecord(ModifierRecord.TYPE_ADJ, ModifierRecord.TARGET_BO);
    eFrag.addMod("following", modRecord3);
    eFrag.boIsSubject = true;
    eFrag.boHasArticle = false;
    eFrag.verbIsPassive = true;
    eFrag.addAssociation(Integer.valueOf(node.getEntry().getId()));

    ArrayList<DSynTSentence> preStatements = new ArrayList<>();
    preStatements.add(new DSynTMainSentence(eFrag));
    return new ConverterRecord(null, null, preStatements, null);
  }

  // *********************************************************************************************
  //										XOR - SPLIT
  // *********************************************************************************************
  ArrayList<DSynTSentence> convertXORSimple(
      RPSTNode<ControlFlow, Node> node, GatewayExtractor gwExtractor) {
    ExecutableFragment eFragYes = null;
    ExecutableFragment eFragNo = null;
    String role = "";

    ArrayList<RPSTNode<ControlFlow, Node>> pNodeList = new ArrayList<>(rpst.getChildren(node));
    for (RPSTNode<ControlFlow, Node> pNode : pNodeList) {
      for (RPSTNode<ControlFlow, Node> tNode : rpst.getChildren(pNode)) {
        if (tNode.getEntry() == node.getEntry()) {
          for (de.dhbw.woped.process2text.model.process.Arc arc : process.getArcs().values()) {
            if (arc.getSource().getId() == Integer.valueOf(tNode.getEntry().getId())
                && arc.getTarget().getId() == Integer.valueOf(tNode.getExit().getId())) {
              if (arc.getLabel().equalsIgnoreCase("yes")) {
                de.dhbw.woped.process2text.model.process.Activity a =
                    process.getActivity(Integer.valueOf(tNode.getExit().getId()));
                de.dhbw.woped.process2text.model.process.Annotation anno =
                    a.getAnnotations().get(0);
                String action = anno.getActions().get(0);
                String bo = anno.getBusinessObjects().get(0);
                role = a.getLane().getName();

                String addition = anno.getAddition();
                eFragYes = new ExecutableFragment(action, bo, role, addition);
                eFragYes.addAssociation(Integer.valueOf(node.getExit().getId()));
              }
              if (arc.getLabel().equalsIgnoreCase("no")) {
                de.dhbw.woped.process2text.model.process.Activity a =
                    process.getActivity(Integer.valueOf(tNode.getExit().getId()));
                de.dhbw.woped.process2text.model.process.Annotation anno =
                    a.getAnnotations().get(0);
                String action = anno.getActions().get(0);
                String bo = anno.getBusinessObjects().get(0);

                role = a.getLane().getName();

                String addition = anno.getAddition();
                eFragNo = new ExecutableFragment(action, bo, role, addition);

                ModifierRecord modRecord =
                    new ModifierRecord(ModifierRecord.TYPE_ADV, ModifierRecord.TARGET_VERB);
                modRecord.addAttribute("adv-type", "sentential");
                eFragNo.addMod("otherwise", modRecord);
                eFragNo.senHasConnective = true;
                eFragNo.addAssociation(Integer.valueOf(node.getExit().getId()));
              }
            }
          }
        }
      }
    }

    ConditionFragment cFrag =
        new ConditionFragment(
            gwExtractor.getVerb(),
            gwExtractor.getObject(),
            "",
            "",
            ConditionFragment.TYPE_IF,
            gwExtractor.getModList());
    cFrag.boReplaceWithPronoun = true;
    cFrag.addAssociation(Integer.valueOf(node.getEntry().getId()));

    // If imperative mode
    if (imperative && imperativeRole.equals(role)) {
      eFragNo.setRole("");
      eFragNo.verbIsImperative = true;
      eFragYes.setRole("");
      eFragYes.verbIsImperative = true;
    }

    DSynTConditionSentence dsyntSentence1 = new DSynTConditionSentence(eFragYes, cFrag);
    DSynTMainSentence dsyntSentence2 = new DSynTMainSentence(eFragNo);
    ArrayList<DSynTSentence> sentences = new ArrayList<>();
    sentences.add(dsyntSentence1);
    sentences.add(dsyntSentence2);
    return sentences;
  }

  ConverterRecord convertXORGeneral(RPSTNode<ControlFlow, Node> node) {
    // One of the following branches is executed.  (And then use bullet points for structuring)
    ExecutableFragment eFrag =
        new ExecutableFragment("execute", "one of the following branches", "", "");
    eFrag.boIsSubject = true;
    eFrag.verbIsPassive = true;
    eFrag.boHasArticle = false;
    eFrag.addAssociation(Integer.valueOf(node.getEntry().getId()));

    ArrayList<DSynTSentence> preStatements = new ArrayList<>();
    preStatements.add(new DSynTMainSentence(eFrag));

    // Statement about negative case (process is finished)
    ConditionFragment post =
        new ConditionFragment(
            "execute",
            "one of the alternative branches",
            "",
            "",
            ConditionFragment.TYPE_ONCE,
            new HashMap<>());
    post.verbIsPast = true;
    post.verbIsPassive = true;
    post.boIsSubject = true;
    post.boIsPlural = false;
    post.boHasArticle = false;
    post.setFragmentType(AbstractFragment.TYPE_JOIN);
    post.addAssociation(Integer.valueOf(node.getEntry().getId()));

    return new ConverterRecord(null, post, preStatements, null, null);
  }

  // *********************************************************************************************
  //										LOOP - SPLIT
  // *********************************************************************************************

  /** Converts a loop construct with labeled entry condition into two sentences. */
  ConverterRecord convertLoop(
      RPSTNode<ControlFlow, Node> node, RPSTNode<ControlFlow, Node> firstActivity) {
    // Labeled Case
    if (!node.getExit().getName().equals("")) {
      // Derive information from the gateway
      GatewayExtractor gwExtractor = new GatewayExtractor(node.getExit(), lHelper);

      // Generate general statement about loop
      //			String  role =
      // process.getGateways().get(Integer.valueOf(node.getEntry().getId())).getLane().getName();
      String role = getRole(node);

      ExecutableFragment eFrag = new ExecutableFragment("repeat", "step", role, "");
      eFrag.addAssociation(Integer.valueOf(node.getEntry().getId()));
      ModifierRecord modRecord =
          new ModifierRecord(ModifierRecord.TYPE_ADJ, ModifierRecord.TARGET_BO);
      eFrag.addMod("latter", modRecord);
      eFrag.boIsPlural = true;

      ExecutableFragment eFrag2 = new ExecutableFragment("continue", "", "", "");
      eFrag2.addAssociation(Integer.valueOf(node.getEntry().getId()));
      eFrag.addSentence(eFrag2);
      if (role.equals("")) {
        eFrag.verbIsPassive = true;
        eFrag.boIsSubject = true;
        eFrag2.verbIsPassive = true;
        eFrag2.setBo("it");
        eFrag2.boIsSubject = true;
        eFrag2.boHasArticle = false;
      }

      de.dhbw.woped.process2text.model.process.Activity a =
          process.getActivity(Integer.valueOf(firstActivity.getExit().getId()));
      ExecutableFragment eFrag3 =
          new ExecutableFragment(
              a.getAnnotations().get(0).getActions().get(0),
              a.getAnnotations().get(0).getBusinessObjects().get(0),
              "",
              "");
      eFrag3.addAssociation(a.getId());
      eFrag3.senIsCoord = false;
      eFrag3.verbIsParticiple = true;
      ModifierRecord modRecord2 =
          new ModifierRecord(ModifierRecord.TYPE_ADV, ModifierRecord.TARGET_VERB);
      modRecord2.addAttribute("adv-type", "sentential");
      eFrag3.addMod("with", modRecord2);
      eFrag2.addSentence(eFrag3);

      ConditionFragment cFrag =
          new ConditionFragment(
              gwExtractor.getVerb(),
              gwExtractor.getObject(),
              "",
              "",
              ConditionFragment.TYPE_AS_LONG_AS,
              new HashMap<>(gwExtractor.getModList()));
      cFrag.verbIsPassive = true;
      cFrag.boIsSubject = true;
      cFrag.senHeadPosition = true;
      cFrag.addAssociation(Integer.valueOf(node.getEntry().getId()));

      // Determine postcondition
      gwExtractor.negateGatewayLabel();
      ConditionFragment post =
          new ConditionFragment(
              gwExtractor.getVerb(),
              gwExtractor.getObject(),
              "",
              "",
              ConditionFragment.TYPE_ONCE,
              gwExtractor.getModList());
      post.verbIsPassive = true;
      post.boIsSubject = true;
      post.setFragmentType(AbstractFragment.TYPE_JOIN);
      post.addAssociation(Integer.valueOf(node.getEntry().getId()));

      // If imperative mode
      if (imperative && imperativeRole.equals(role)) {
        eFrag.setRole("");
        eFrag.verbIsImperative = true;
        eFrag2.verbIsImperative = true;
      }

      ArrayList<DSynTSentence> postStatements = new ArrayList<>();
      postStatements.add(new DSynTConditionSentence(eFrag, cFrag));
      return new ConverterRecord(null, post, null, postStatements);
    }
    // Unlabeled case
    else {
      ExecutableFragment eFrag = new ExecutableFragment("repeat", "step", "", "");
      ModifierRecord modRecord =
          new ModifierRecord(ModifierRecord.TYPE_ADJ, ModifierRecord.TARGET_BO);
      eFrag.addMod("latter", modRecord);
      eFrag.boIsPlural = true;
      eFrag.boIsSubject = true;
      eFrag.verbIsPassive = true;
      ExecutableFragment eFrag2 = new ExecutableFragment("continue", "", "", "");
      eFrag.addSentence(eFrag2);

      de.dhbw.woped.process2text.model.process.Activity a =
          process.getActivity(Integer.valueOf(firstActivity.getExit().getId()));

      // Loops require roles --> If no role is set, the catch block sets a default role 'Process'
      String role;
      try {
        role = a.getLane().getName();
        if (role.equals("")) {
          role = a.getPool().getName();
        }
      } catch (NullPointerException e) {
        role = "process";
        // Make sure to use roles when handeling loops. Default Role 'Process' used now.");
      }

      eFrag2.setRole(role);
      ExecutableFragment eFrag3 =
          new ExecutableFragment(
              a.getAnnotations().get(0).getActions().get(0),
              a.getAnnotations().get(0).getBusinessObjects().get(0),
              "",
              a.getAnnotations().get(0).getAddition());
      eFrag3.senIsCoord = false;
      eFrag3.verbIsParticiple = true;
      ModifierRecord modRecord2 =
          new ModifierRecord(ModifierRecord.TYPE_ADV, ModifierRecord.TARGET_VERB);
      modRecord2.addAttribute("adv-type", "sentential");
      eFrag3.addMod("with", modRecord2);
      eFrag2.addSentence(eFrag3);

      ConditionFragment cFrag =
          new ConditionFragment("be", "dummy", "", "", ConditionFragment.TYPE_IF, new HashMap<>());
      cFrag.addMod(
          "required", new ModifierRecord(ModifierRecord.TYPE_ADV, ModifierRecord.TARGET_VERB));
      cFrag.boReplaceWithPronoun = true;
      cFrag.boIsSubject = true;
      cFrag.senHeadPosition = true;

      // Determine postcondition
      ConditionFragment post =
          new ConditionFragment(
              "finish", "loop", "", "", ConditionFragment.TYPE_ONCE, new HashMap<>());
      post.verbIsPassive = true;
      post.boIsSubject = true;
      post.setFragmentType(AbstractFragment.TYPE_JOIN);

      // If imperative mode
      if (imperative && imperativeRole.equals(role)) {
        eFrag.setRole("");
        eFrag.verbIsImperative = true;
        eFrag2.verbIsImperative = true;
      }

      ArrayList<DSynTSentence> postStatements = new ArrayList<>();
      postStatements.add(new DSynTConditionSentence(eFrag, cFrag));
      return new ConverterRecord(null, post, null, postStatements);
    }
  }

  // *********************************************************************************************
  //										SKIP - SPLIT
  // *********************************************************************************************

  ConverterRecord convertSkipGeneralUnlabeled(RPSTNode<ControlFlow, Node> node) {
    ConditionFragment pre =
        new ConditionFragment("be", "dummy", "", "", ConditionFragment.TYPE_IF, new HashMap<>());
    ModifierRecord mod = new ModifierRecord(ModifierRecord.TYPE_ADV, ModifierRecord.TARGET_VERB);
    pre.addMod("necessary", mod);
    pre.boReplaceWithPronoun = true;
    pre.senHeadPosition = true;
    pre.senIsCoord = true;
    pre.senHasComma = true;
    pre.addAssociation(Integer.valueOf(node.getEntry().getId()));
    return new ConverterRecord(pre, null, null, null);
  }

  /** Converts a standard skip construct with labeled condition gateway into two sentences. */
  ConverterRecord convertSkipGeneral(RPSTNode<ControlFlow, Node> node) {
    // Derive information from the gateway
    GatewayExtractor gwExtractor = new GatewayExtractor(node.getEntry(), lHelper);

    // Generate general statement about upcoming decision
    ConditionFragment pre =
        new ConditionFragment(
            gwExtractor.getVerb(),
            gwExtractor.getObject(),
            "",
            "",
            ConditionFragment.TYPE_IN_CASE,
            gwExtractor.getModList());
    pre.verbIsPassive = gwExtractor.hasVerb;
    pre.boIsSubject = true;
    pre.senHeadPosition = true;
    pre.boIsPlural = gwExtractor.boIsPlural;
    pre.boHasArticle = gwExtractor.boHasArticle;
    pre.addAssociation(Integer.valueOf(node.getEntry().getId()));
    return new ConverterRecord(pre, null, null, null);
  }

  /**
   * Converts a standard skip construct with labeled condition gateway, leading to the end of the
   * process, into two sentences.
   */
  ConverterRecord convertSkipToEnd(RPSTNode<ControlFlow, Node> node) {
    // Derive information from the gateway
    GatewayExtractor gwExtractor = new GatewayExtractor(node.getEntry(), lHelper);
    String role = getRole(node);

    // Generate general statement about upcoming decision
    ExecutableFragment eFrag = new ExecutableFragment("decide", "", role, "");
    ConditionFragment cFrag =
        new ConditionFragment(
            gwExtractor.getVerb(),
            gwExtractor.getObject(),
            "",
            "",
            ConditionFragment.TYPE_WHETHER,
            gwExtractor.getModList());
    cFrag.verbIsPassive = true;
    cFrag.boIsSubject = true;
    cFrag.senHeadPosition = false;
    cFrag.addAssociation(Integer.valueOf(node.getEntry().getId()));
    eFrag.addAssociation(Integer.valueOf(node.getEntry().getId()));

    if (role.equals("")) {
      eFrag.verbIsPassive = true;
      eFrag.setBo("it");
      eFrag.boHasArticle = false;
      eFrag.boIsSubject = true;
      cFrag.verbIsPassive = true;
      cFrag.setBo("it");
      cFrag.boHasArticle = false;
      cFrag.boIsSubject = true;
    }

    // Statement about negative case (process is finished)
    ExecutableFragment eFrag2 = new ExecutableFragment("finish", "process instance", "", "");
    eFrag2.verbIsPassive = true;
    eFrag2.boIsSubject = true;
    ConditionFragment cFrag2 =
        new ConditionFragment("be", "case", "this", "", ConditionFragment.TYPE_IF, new HashMap<>());
    cFrag2.verbIsNegated = true;

    // Determine precondition
    ConditionFragment pre =
        new ConditionFragment(
            gwExtractor.getVerb(),
            gwExtractor.getObject(),
            "",
            "",
            ConditionFragment.TYPE_IF,
            new HashMap<>());
    pre.verbIsPassive = true;
    pre.senHeadPosition = true;
    pre.boIsSubject = true;
    ModifierRecord modRecord =
        new ModifierRecord(ModifierRecord.TYPE_PREP, ModifierRecord.TARGET_VERB);
    modRecord.addAttribute("adv-type", "sentential");
    pre.addMod("otherwise", modRecord);
    pre.senHasConnective = true;

    // If imperative mode
    if (imperative && imperativeRole.equals(role)) {
      eFrag.setRole("");
      eFrag.verbIsImperative = true;
    }

    ArrayList<DSynTSentence> preStatements = new ArrayList<>();
    preStatements.add(new DSynTConditionSentence(eFrag, cFrag));
    preStatements.add(new DSynTConditionSentence(eFrag2, cFrag2));

    return new ConverterRecord(pre, null, preStatements, null);
  }

  // *********************************************************************************************
  //										AND - SPLIT
  // *********************************************************************************************

  ConverterRecord convertANDGeneral(RPSTNode<ControlFlow, Node> node, int activities) {

    // The process is split into three parallel branches.  (And then use bullet points for
    // structuring)
    ExecutableFragment eFrag =
        new ExecutableFragment("split", "process", "", "into " + activities + " parallel branches");
    eFrag.boIsSubject = true;
    eFrag.verbIsPassive = true;
    eFrag.addHasArticle = false;
    eFrag.addAssociation(Integer.valueOf(node.getEntry().getId()));

    ArrayList<DSynTSentence> preStatements = new ArrayList<>();
    preStatements.add(new DSynTMainSentence(eFrag));

    // Statement about negative case (process is finished)
    ConditionFragment post =
        new ConditionFragment(
            "execute",
            "all " + activities + " branch",
            "",
            "",
            ConditionFragment.TYPE_ONCE,
            new HashMap<>());
    post.verbIsPast = true;
    post.verbIsPassive = true;
    post.boIsSubject = true;
    post.boIsPlural = true;
    post.boHasArticle = false;
    post.addAssociation(Integer.valueOf(node.getEntry().getId()));
    post.setFragmentType(AbstractFragment.TYPE_JOIN);

    return new ConverterRecord(null, post, preStatements, null, null);
  }

  /** Converts a simple and construct. */
  ConverterRecord convertANDSimple(
      RPSTNode<ControlFlow, Node> node, int activities, ArrayList<Node> conditionNodes) {
    // get last element of both branches and combine them to a post condition
    // if one of them is a gateway, include gateway post condition in the and post condition
    ModifierRecord modRecord =
        new ModifierRecord(ModifierRecord.TYPE_ADV, ModifierRecord.TARGET_VERB);
    modRecord.addAttribute("adv-type", "sentential");

    if (activities == 1) {
      modRecord.setLemma("In concurrency to the latter step,");
    } else {
      modRecord.setLemma("In concurrency to the latter " + activities + " steps,");
    }

    // Determine postcondition
    ConditionFragment post = null;
    String role = "";

    // Check whether postcondition should be passed
    int arcs = 0;
    for (de.dhbw.woped.process2text.model.process.Arc arc : process.getArcs().values()) {
      if (arc.getTarget().getId() == Integer.valueOf(node.getExit().getId())) {
        arcs++;
      }
    }

    // Only of no other arc flows into join gateway, join condition is passed
    if (arcs == 2) {
      if (conditionNodes.size() == 1) {
        de.dhbw.woped.process2text.model.process.Activity a =
            process.getActivity(Integer.valueOf(conditionNodes.get(0).getId()));
        String verb = a.getAnnotations().get(0).getActions().get(0);
        role = getRole(node);
        post =
            new ConditionFragment(
                "finish",
                lHelper.getNoun(verb),
                role,
                "",
                ConditionFragment.TYPE_ONCE,
                new HashMap<>());
        post.senHeadPosition = true;
        post.verbIsPast = true;
        post.setFragmentType(AbstractFragment.TYPE_JOIN);
        post.addAssociation(Integer.valueOf(node.getEntry().getId()));
      } else {
        post =
            new ConditionFragment(
                "finish", "both branch", "", "", ConditionFragment.TYPE_ONCE, new HashMap<>());
        post.boIsPlural = true;
        post.senHeadPosition = true;
        post.boHasArticle = false;
        post.boIsSubject = true;
        post.verbIsPast = true;
        post.verbIsPassive = true;
        post.setFragmentType(AbstractFragment.TYPE_JOIN);
        post.addAssociation(Integer.valueOf(node.getEntry().getId()));
      }
    }

    // If imperative mode
    if (imperative && imperativeRole.equals(role)) {
      post.roleIsImperative = true;
    }

    return new ConverterRecord(null, post, null, null, modRecord);
  }

  ConverterRecord convertEvent(de.dhbw.woped.process2text.model.process.Event event) {
    ConditionFragment cFrag;
    ExecutableFragment eFrag;
    ArrayList<DSynTSentence> preSentences;
    switch (event.getType()) {

      // ***************************************************************
      // 				INTERMEDIATE (CATCHING) EVENTS
      // ***************************************************************

      // ERROR EVENT
      case de.dhbw.woped.process2text.model.process.EventType.INTM_ERROR:
        String error = event.getLabel();

        if (error.equals("")) {
          cFrag =
              new ConditionFragment(
                  "occur", "error", "", "", ConditionFragment.TYPE_IF, new HashMap<>());
          cFrag.boHasIndefArticle = true;
        } else {
          cFrag =
              new ConditionFragment(
                  "occur",
                  "error '" + error + "'",
                  "",
                  "",
                  ConditionFragment.TYPE_IF,
                  new HashMap<>());
          cFrag.boHasArticle = true;
        }
        cFrag.boIsSubject = true;
        if (event.isAttached()) {
          cFrag.setAddition("while latter task is executed,");
        }
        break;

      // TIMER EVENT
      case de.dhbw.woped.process2text.model.process.EventType.INTM_TIMER:
        String limit = event.getLabel();
        if (limit.equals("")) {
          cFrag =
              new ConditionFragment(
                  "reach", "the time limit", "", "", ConditionFragment.TYPE_IF, new HashMap<>());
        } else {
          cFrag =
              new ConditionFragment(
                  "reach",
                  "the time limit of " + limit,
                  "",
                  "",
                  ConditionFragment.TYPE_IF,
                  new HashMap<>());
        }

        if (event.isAttached()) {
          cFrag.setAddition("while latter task is executed,");
        }
        configureFragment(cFrag);
        break;

      // MESSAGE EVENT (CATCHING)
      case de.dhbw.woped.process2text.model.process.EventType.INTM_MSG_CAT:
        cFrag =
            new ConditionFragment(
                "receive", "a message", "", "", ConditionFragment.TYPE_ONCE, new HashMap<>());
        configureFragment(cFrag);
        break;

      // ESCALATION EVENT (CATCHING)
      case de.dhbw.woped.process2text.model.process.EventType.INTM_ESCALATION_CAT:
        cFrag =
            new ConditionFragment(
                "", "of an escalation", "", "", ConditionFragment.TYPE_IN_CASE, new HashMap<>());
        cFrag.boHasArticle = false;
        cFrag.boIsSubject = true;
        break;

      // ***************************************************************
      // 						START / END EVENTS
      // ***************************************************************

      // END EVENT
      case de.dhbw.woped.process2text.model.process.EventType.END_EVENT:
        eFrag = new ExecutableFragment("finish", "process", "", "");
        eFrag.verbIsPassive = true;
        eFrag.boIsSubject = true;
        eFrag.boHasArticle = true;
        return getEventSentence(eFrag);

      // ERROR EVENT
      case de.dhbw.woped.process2text.model.process.EventType.END_ERROR:
        eFrag = new ExecutableFragment("end", "process", "", "with an error");
        eFrag.boIsSubject = true;
        eFrag.boHasArticle = true;
        eFrag.addHasArticle = false;
        return getEventSentence(eFrag);

      case de.dhbw.woped.process2text.model.process.EventType.END_SIGNAL:
        eFrag = new ExecutableFragment("end", "process", "", "with a signal.");
        eFrag.boIsSubject = true;
        eFrag.boHasArticle = true;
        eFrag.addHasArticle = false;
        return getEventSentence(eFrag);

      // START EVENT
      case de.dhbw.woped.process2text.model.process.EventType.START_MSG:
        cFrag = new ConditionFragment("receive", "message", "", "", ConditionFragment.TYPE_ONCE);
        cFrag.boIsSubject = true;
        cFrag.verbIsPassive = true;
        cFrag.boHasArticle = true;
        cFrag.boHasIndefArticle = true;
        eFrag = new ExecutableFragment("start", "process", "", "");
        eFrag.boIsSubject = true;
        eFrag.boHasArticle = true;
        return getEventSentence(eFrag, cFrag);

      // ***************************************************************
      // 						THROWING EVENTS
      // ***************************************************************

      // MESSAGE EVENT
      case de.dhbw.woped.process2text.model.process.EventType.INTM_MSG_THR:
        eFrag = new ExecutableFragment("send", "message", event.getLane().getName(), "");
        eFrag.boHasIndefArticle = true;
        return getEventSentence(eFrag);

      // ESCALATION EVENT
      case de.dhbw.woped.process2text.model.process.EventType.INTM_ESCALATION_THR:
        eFrag = new ExecutableFragment("trigger", "escalation", event.getLane().getName(), "");
        eFrag.boHasIndefArticle = true;
        return getEventSentence(eFrag);

      // LINK EVENT
      case de.dhbw.woped.process2text.model.process.EventType.INTM_LINK_THR:
        eFrag = new ExecutableFragment("send", "signal", event.getLane().getName(), "");
        eFrag.boHasIndefArticle = true;
        return getEventSentence(eFrag);

      // MULTIPLE TRIGGER
      case de.dhbw.woped.process2text.model.process.EventType.INTM_MULTIPLE_THR:
        eFrag = new ExecutableFragment("cause", "multiple trigger", event.getLane().getName(), "");
        eFrag.boHasArticle = false;
        eFrag.boIsPlural = true;
        return getEventSentence(eFrag);

      // SIGNAL EVENT
      case de.dhbw.woped.process2text.model.process.EventType.INTM_SIGNAL_THR:
        eFrag = new ExecutableFragment("send", "signal", event.getLane().getName(), "");
        eFrag.boHasArticle = true;
        eFrag.boHasIndefArticle = true;
        eFrag.boIsPlural = true;
        return getEventSentence(eFrag);

      default:
        logger.info("NON-COVERED EVENT " + event.getType());
        return null;
    }

    // Handling of intermediate Events (up until now only condition is provided)

    // Attached Event
    if (event.isAttached()) {
      preSentences = new ArrayList<>();
      preSentences.add(getAttachedEventSentence(event, cFrag));
      return new ConverterRecord(null, null, preSentences, null);

      // Non-attached Event
    } else {
      preSentences = new ArrayList<>();
      preSentences.add(getIntermediateEventSentence(cFrag));
      return new ConverterRecord(null, null, preSentences, null);
    }
  }

  /** Returns Sentence for attached Event. */
  private DSynTConditionSentence getAttachedEventSentence(
      de.dhbw.woped.process2text.model.process.Event event, ConditionFragment cFrag) {
    ExecutableFragment eFrag = new ExecutableFragment("cancel", "it", "", "");
    eFrag.verbIsPassive = true;
    eFrag.boIsSubject = true;
    eFrag.boHasArticle = false;

    if (!event.isLeadsToEnd()) {
      ModifierRecord modRecord =
          new ModifierRecord(ModifierRecord.TYPE_ADV, ModifierRecord.TARGET_VERB);
      ExecutableFragment eFrag2 = new ExecutableFragment("continue", "process", "", "");
      modRecord.addAttribute("adv-type", "sent-final");
      modRecord.addAttribute("rheme", "+");
      eFrag2.addMod("as follows", modRecord);

      eFrag2.boIsSubject = true;
      eFrag.addSentence(eFrag2);
    } else {
      ExecutableFragment eFrag2 = new ExecutableFragment("finish", "process", "", "");
      eFrag2.boIsSubject = true;
      eFrag2.verbIsPassive = true;
      eFrag.addSentence(eFrag2);
    }
    return new DSynTConditionSentence(eFrag, cFrag);
  }

  // For attached events only
  DSynTConditionSentence getAttachedEventPostStatement(
      de.dhbw.woped.process2text.model.process.Event event) {
    ModifierRecord modRecord;
    ModifierRecord modRecord2;
    ExecutableFragment eFrag;
    ConditionFragment cFrag;

    switch (event.getType()) {
      case de.dhbw.woped.process2text.model.process.EventType.INTM_TIMER:
        modRecord = new ModifierRecord(ModifierRecord.TYPE_ADV, ModifierRecord.TARGET_VERB);
        eFrag = new ExecutableFragment("continue", "process", "", "");
        eFrag.boIsSubject = true;
        modRecord.addAttribute("adv-type", "sent-final");
        modRecord.addAttribute("rheme", "+");
        eFrag.addMod("normally", modRecord);

        cFrag =
            new ConditionFragment(
                "complete",
                "the task",
                "",
                "within the time limit",
                ConditionFragment.TYPE_IF,
                new HashMap<>());
        cFrag.senHasConnective = true;
        cFrag.addHasArticle = false;
        modRecord2 = new ModifierRecord(ModifierRecord.TYPE_PREP, ModifierRecord.TARGET_VERB);
        modRecord2.addAttribute("adv-type", "sentential");
        cFrag.addMod("otherwise", modRecord2);
        configureFragment(cFrag);
        return new DSynTConditionSentence(eFrag, cFrag);
      case de.dhbw.woped.process2text.model.process.EventType.INTM_ERROR:
        modRecord = new ModifierRecord(ModifierRecord.TYPE_ADV, ModifierRecord.TARGET_VERB);
        eFrag = new ExecutableFragment("continue", "process", "", "");
        eFrag.boIsSubject = true;
        modRecord.addAttribute("adv-type", "sent-final");
        modRecord.addAttribute("rheme", "+");
        eFrag.addMod("normally", modRecord);

        cFrag =
            new ConditionFragment(
                "complete",
                "the task",
                "",
                "without error",
                ConditionFragment.TYPE_IF,
                new HashMap<>());
        cFrag.senHasConnective = true;
        cFrag.addHasArticle = false;
        modRecord2 = new ModifierRecord(ModifierRecord.TYPE_PREP, ModifierRecord.TARGET_VERB);
        modRecord2.addAttribute("adv-type", "sentential");
        cFrag.addMod("otherwise", modRecord2);
        configureFragment(cFrag);
        return new DSynTConditionSentence(eFrag, cFrag);
      case de.dhbw.woped.process2text.model.process.EventType.INTM_ESCALATION_CAT:
        modRecord = new ModifierRecord(ModifierRecord.TYPE_ADV, ModifierRecord.TARGET_VERB);
        eFrag = new ExecutableFragment("continue", "process", "", "");
        eFrag.boIsSubject = true;
        modRecord.addAttribute("adv-type", "sent-final");
        modRecord.addAttribute("rheme", "+");
        eFrag.addMod("normally", modRecord);

        cFrag =
            new ConditionFragment(
                "complete",
                "the task",
                "",
                "without escalation",
                ConditionFragment.TYPE_IF,
                new HashMap<>());
        cFrag.senHasConnective = true;
        cFrag.addHasArticle = false;
        modRecord2 = new ModifierRecord(ModifierRecord.TYPE_PREP, ModifierRecord.TARGET_VERB);
        modRecord2.addAttribute("adv-type", "sentential");
        cFrag.addMod("otherwise", modRecord2);
        configureFragment(cFrag);
        return new DSynTConditionSentence(eFrag, cFrag);
      default:
        logger.info("NON-COVERED EVENT " + event.getType());
        return null;
    }
  }

  /** Returns record with sentence for throwing events. */
  private ConverterRecord getEventSentence(ExecutableFragment eFrag) {
    DSynTMainSentence msen = new DSynTMainSentence(eFrag);
    ArrayList<DSynTSentence> preSentences = new ArrayList<>();
    preSentences.add(msen);
    return new ConverterRecord(null, null, preSentences, null);
  }

  private ConverterRecord getEventSentence(ExecutableFragment eFrag, ConditionFragment cFrag) {
    DSynTConditionSentence msen = new DSynTConditionSentence(eFrag, cFrag);
    ArrayList<DSynTSentence> preSentences = new ArrayList<>();
    preSentences.add(msen);
    return new ConverterRecord(null, null, preSentences, null);
  }

  /** Returns sentence for intermediate events. */
  private DSynTConditionSentence getIntermediateEventSentence(ConditionFragment cFrag) {
    ExecutableFragment eFrag = new ExecutableFragment("continue", "process", "", "");
    eFrag.boIsSubject = true;
    return new DSynTConditionSentence(eFrag, cFrag);
  }

  /** Configures condition fragment in a standard fashion. */
  private void configureFragment(ConditionFragment cFrag) {
    cFrag.verbIsPassive = true;
    cFrag.boIsSubject = true;
    cFrag.boHasArticle = false;
  }

  /** Returns role executing current RPST node. */
  private String getRole(RPSTNode<ControlFlow, Node> node) {
    String role =
        process.getGateways().get(Integer.valueOf(node.getExit().getId())).getLane().getName();
    if (role.equals("")) {
      role = process.getGateways().get(Integer.valueOf(node.getExit().getId())).getPool().getName();
    }
    return role;
  }
}
