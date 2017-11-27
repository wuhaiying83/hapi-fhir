package org.hl7.fhir.r4.utils.transform;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.exceptions.DefinitionException;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.context.IWorkerContext;
import org.hl7.fhir.r4.elementmodel.Property;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.utils.NarrativeGenerator;
import org.hl7.fhir.utilities.Utilities;
import org.hl7.fhir.utilities.xhtml.XhtmlNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.UUID;

import static org.hl7.fhir.r4.utils.transform.FhirTransformationEngine.*;

/**
 * Structure Map Rule Runner class
 */
public class StructureMapRuleRunner extends BaseRunner {

  /**
   * Rule Component for a structure map
   */
  private StructureMap.StructureMapGroupRuleComponent rule;

  /**
   * Parent group
   */
  private StructureMap.StructureMapGroupComponent parentGroup;
  private Stack<StructureMap.StructureMapGroupRuleComponent> ruleStack;
  private StructureMapGroupHandler parentGroupRunner;
  private FhirTransformationEngine transformationEngine;

  /**
   * Constructor
   *
   * @param map                  StructureMap
   * @param transformationEngine FhirTransformationEngine
   * @param parentGroupRunner    StructureMapGroupRunner
   * @param rule                 RuleComponent
   */
  @SuppressWarnings("WeakerAccess")
  public StructureMapRuleRunner(StructureMap map, FhirTransformationEngine transformationEngine, StructureMapGroupHandler parentGroupRunner, StructureMap.StructureMapGroupRuleComponent rule) {
    setStructureMap(map);
    setRule(rule);
    setParentGroupRunner(parentGroupRunner);
    setTransformationEngine(transformationEngine);
    setParentGroup(parentGroupRunner.getRuleGroup());
  }

  /**
   * Constructor
   *
   * @param map                  StructureMap
   * @param transformationEngine Transformation Engine
   * @param parentGroupRunner    Group Runner
   * @param rule                 Rule Component
   * @param worker               Worker Context
   */
  @SuppressWarnings("WeakerAccess")
  public StructureMapRuleRunner(StructureMap map, FhirTransformationEngine transformationEngine, StructureMapGroupHandler parentGroupRunner, StructureMap.StructureMapGroupRuleComponent rule, IWorkerContext worker) {
    this(map, transformationEngine, parentGroupRunner, rule);
    setWorker(worker);
  }

  /**
   * get accessor for rule
   *
   * @return rule StructureMapGroupRuleComponent value
   */
  public StructureMap.StructureMapGroupRuleComponent getRule() {
    return rule;
  }

  /**
   * set accessor for rule
   *
   * @param rule rule StructureMapGroupRuleComponent value
   */
  public void setRule(StructureMap.StructureMapGroupRuleComponent rule) {
    this.rule = rule;
  }

  /**
   * get accessor for parentGroup
   *
   * @return parentGroup StructureMapGroupComponent value
   */
  @SuppressWarnings("unused")
  public StructureMap.StructureMapGroupComponent getParentGroup() {
    return parentGroup;
  }

  /**
   * set accessor for parentGroup
   *
   * @param parentGroup parentGroup StructureMapGroupComponent value
   */
  @SuppressWarnings("WeakerAccess")
  public void setParentGroup(StructureMap.StructureMapGroupComponent parentGroup) {
    this.parentGroup = parentGroup;
  }

  /**
   * get accessor for parentGroupRunner
   *
   * @return parentGroupRunner StructureMapGroupHandler value
   */
  @SuppressWarnings("unused")
  public StructureMapGroupHandler getParentGroupRunner() {
    return parentGroupRunner;
  }

  /**
   * set accessor for parentGroupRunner
   *
   * @param parentGroupRunner parentGroupRunner StructureMapGroupHandler value
   */
  @SuppressWarnings("WeakerAccess")
  public void setParentGroupRunner(StructureMapGroupHandler parentGroupRunner) {
    this.parentGroupRunner = parentGroupRunner;
  }

  /**
   * get accessor for transformationEngine
   *
   * @return transformationEngine FhirTransformationEngine value
   */
  @SuppressWarnings("WeakerAccess")
  public FhirTransformationEngine getTransformationEngine() {
    return transformationEngine;
  }

  /**
   * set accessor for transformationEngine
   *
   * @param transformationEngine transform Engine value
   */
  @SuppressWarnings("WeakerAccess")
  public void setTransformationEngine(FhirTransformationEngine transformationEngine) {
    this.transformationEngine = transformationEngine;
  }

  protected void initialize() {

  }

  /*
   * Method processes a rule.
   * <p>At this time, a rule can only have a single source.</p>
   *
   * @param indent
   * @param context
   * @param result
   * @throws Exception
   */

  /**
   * Method processes a rule.
   * <p>At this time, a rule can only have a single source.</p>
   *
   * @param context BatchContext
   * @param indent  whitespace string
   * @param result  Analysis result
   * @param vars    Variables for Profiling
   * @throws Exception if arguments are invalid or other runtime exception occurs
   */
  @SuppressWarnings("WeakerAccess")
  protected void analyseRule(BatchContext context, String indent, StructureMapAnalysis result, VariablesForProfiling vars) throws Exception {
    log(indent + "Analyse rule : " + rule.getName());
    XhtmlNode tr = result.getSummary().addTag("tr");
    XhtmlNode xs = tr.addTag("td");
    XhtmlNode xt = tr.addTag("td");

    VariablesForProfiling srcVars = vars.copy();
    if (rule.getSource().size() != 1) {
      throw new UnsupportedOperationException("Unsupported configuration at this time: Rule \"" + rule.getName() + "\": declares more than one source input.");
    }
    VariablesForProfiling source = analyseSource(context, rule.getName(), srcVars, rule.getSourceFirstRep(), xs);//TODO Add support for more than one source

    TargetWriter tw = new TargetWriter();
    for (StructureMap.StructureMapGroupRuleTargetComponent t : rule.getTarget()) {
      analyseTarget(context, rule.getName(), source, getStructureMap(), t, rule.getSourceFirstRep().getVariable(), tw, result.getProfiles(), rule.getName());
    }
    tw.commit(xt);

    for (StructureMap.StructureMapGroupRuleComponent childRule : rule.getRule()) {
      if (ruleStack == null) {
        ruleStack = new Stack<>();
      }
      ruleStack.push(rule);
      rule = childRule;
      analyseRule(context, indent + "  ", result, source);
      rule = ruleStack.pop();
    }
//    for (StructureMapGroupRuleDependentComponent dependent : rule.getDependent()) {
//      executeDependency(indent+"  ", context, map, v, group, dependent); // do we need group here?
//    }
  }

  /**
   * Performs rule execution
   *
   * @param indent  whitespace string value
   * @param context Transform Context
   * @param vars    Variables
   * @param group   Group Component
   * @param rule    Rule Component
   * @throws FHIRException if exception occurs within Fhir objects
   */
  @SuppressWarnings({"WeakerAccess", "SpellCheckingInspection"})
  protected void executeRule(String indent, TransformContext context, Variables vars, StructureMap.StructureMapGroupComponent group, StructureMap.StructureMapGroupRuleComponent rule) throws FHIRException {
    log(indent + "rule : " + rule.getName());
    if (rule.getName().contains("CarePlan.participant-unlink"))
      System.out.println("debug");
    Variables srcVars = vars.copy();
    if (rule.getSource().size() != 1)
      throw new FHIRException("Rule \"" + rule.getName() + "\": not handled yet");
    List<Variables> source = processSource(rule.getName(), context, srcVars, rule.getSource().get(0));
    if (source != null) {
      for (Variables v : source) {
        for (StructureMap.StructureMapGroupRuleTargetComponent t : rule.getTarget()) {
          processTarget(rule.getName(), context, v, getStructureMap(), group, t, rule.getSource().size() == 1 ? rule.getSourceFirstRep().getVariable() : null);
        }
        if (rule.hasRule()) {
          for (StructureMap.StructureMapGroupRuleComponent childRule : rule.getRule()) {
            executeRule(indent + "  ", context, v, group, childRule);
          }
        } else if (rule.hasDependent()) {
          for (StructureMap.StructureMapGroupRuleDependentComponent dependent : rule.getDependent()) {
            executeDependency(indent + "  ", context, getStructureMap(), v, group, dependent);
          }
        } else if (rule.getSource().size() == 1 && rule.getSourceFirstRep().hasVariable() && rule.getTarget().size() == 1 && rule.getTargetFirstRep().hasVariable() && rule.getTargetFirstRep().getTransform() == StructureMap.StructureMapTransform.CREATE && !rule.getTargetFirstRep().hasParameter()) {
          // simple inferred, map by type
          Base src = v.get(VariableMode.INPUT, rule.getSourceFirstRep().getVariable());
          Base tgt = v.get(VariableMode.OUTPUT, rule.getTargetFirstRep().getVariable());
          String srcType = src.fhirType();
          String tgtType = tgt.fhirType();
          ResolvedGroup defGroup = resolveGroupByTypes(getStructureMap(), rule.getName(), group, srcType, tgtType);
          Variables vdef = new Variables();
          vdef.add(VariableMode.INPUT, defGroup.target.getInput().get(0).getName(), src);
          vdef.add(VariableMode.OUTPUT, defGroup.target.getInput().get(1).getName(), tgt);
          StructureMapGroupHandler groupRunner = new StructureMapGroupHandler(defGroup.targetMap, getTransformationEngine(), defGroup.target);
          groupRunner.executeGroup(indent + "  ", context, vdef);
        }
      }
    }
  }

  /**
   * Executes a map dependency
   *
   * @param indent    whitespace string value
   * @param context   Transform context
   * @param map       Structure Map
   * @param vars      Variables
   * @param group     Group Component
   * @param dependent Dependant Component
   * @throws FHIRException if exception occurs within Fhir objects
   */
  private void executeDependency(String indent, TransformContext context, StructureMap map, Variables vars, StructureMap.StructureMapGroupComponent group, StructureMap.StructureMapGroupRuleDependentComponent dependent) throws FHIRException {
    ResolvedGroup rg = resolveGroupReference(map, group, dependent.getName());

    if (rg.target.getInput().size() != dependent.getVariable().size()) {
      throw new FHIRException("Rule '" + dependent.getName() + "' has " + Integer.toString(rg.target.getInput().size()) + " but the invocation has " + Integer.toString(dependent.getVariable().size()) + " variables");
    }
    Variables v = new Variables();
    for (int i = 0; i < rg.target.getInput().size(); i++) {
      StructureMap.StructureMapGroupInputComponent input = rg.target.getInput().get(i);
      StringType rdp = dependent.getVariable().get(i);
      String var = rdp.asStringValue();
      VariableMode mode = input.getMode() == StructureMap.StructureMapInputMode.SOURCE ? VariableMode.INPUT : VariableMode.OUTPUT;
      Base vv = vars.get(mode, var);
      if (vv == null && mode == VariableMode.INPUT) //* once source, always source. but target can be treated as source at user convenient
        vv = vars.get(VariableMode.OUTPUT, var);
      if (vv == null)
        throw new FHIRException("Rule '" + dependent.getName() + "' " + mode.toString() + " variable '" + input.getName() + "' named as '" + var + "' has no value");
      v.add(mode, input.getName(), vv);
    }
    StructureMapGroupHandler groupRunner = new StructureMapGroupHandler(rg.targetMap, getTransformationEngine(), rg.target);
    groupRunner.executeGroup(indent + "  ", context, v);
  }

  /**
   * @param map     Structure Map
   * @param ruleId  string Value
   * @param source  Group Component
   * @param srcType string Value
   * @param tgtType string Value
   * @return Resolved Group
   * @throws FHIRException if exception occurs within Fhir objects
   */
  private ResolvedGroup resolveGroupByTypes(StructureMap map, String ruleId, StructureMap.StructureMapGroupComponent source, String srcType, String tgtType) throws FHIRException {
    String kn = "types^" + srcType + ":" + tgtType;
    if (source.hasUserData(kn))
      return (ResolvedGroup) source.getUserData(kn);

    ResolvedGroup res = new ResolvedGroup();
    res.targetMap = null;
    res.target = null;
    for (StructureMap.StructureMapGroupComponent grp : map.getGroup()) {
      if (matchesByType(map, grp, srcType, tgtType)) {
        if (res.targetMap == null) {
          res.targetMap = map;
          res.target = grp;
        } else
          throw new FHIRException("Multiple possible matches looking for rule for '" + srcType + "/" + tgtType + "', from rule '" + ruleId + "'");
      }
    }
    if (res.targetMap != null) {
      source.setUserData(kn, res);
      return res;
    }

    for (UriType imp : map.getImport()) {
      List<StructureMap> impMapList = findMatchingMaps(imp.getValue());
      if (impMapList.size() == 0)
        throw new FHIRException("Unable to find map(s) for " + imp.getValue());
      for (StructureMap impMap : impMapList) {
        if (!impMap.getUrl().equals(map.getUrl())) {
          for (StructureMap.StructureMapGroupComponent grp : impMap.getGroup()) {
            if (matchesByType(impMap, grp, srcType, tgtType)) {
              if (res.targetMap == null) {
                res.targetMap = impMap;
                res.target = grp;
              } else
                throw new FHIRException("Multiple possible matches for rule for '" + srcType + "/" + tgtType + "' in " + res.targetMap.getUrl() + " and " + impMap.getUrl() + ", from rule '" + ruleId + "'");
            }
          }
        }
      }
    }
    if (res.target == null)
      throw new FHIRException("No matches found for rule for '" + srcType + "/" + tgtType + "' from " + map.getUrl() + ", from rule '" + ruleId + "'");
    source.setUserData(kn, res);
    return res;
  }

  /**
   * Analysis source component producing variables
   *
   * @param context Batch Context
   * @param ruleId  String value
   * @param vars    Variables for Profiling
   * @param src     Source Component
   * @param td      XHTML Node
   * @return resulting Variables
   * @throws Exception if error occurs from one of the arguments
   */
  @SuppressWarnings({"WeakerAccess", "ConstantConditions", "unused"})
  protected VariablesForProfiling analyseSource(BatchContext context, String ruleId, VariablesForProfiling vars, StructureMap.StructureMapGroupRuleSourceComponent src, XhtmlNode td) throws Exception {
    VariableForProfiling var = vars.get(VariableMode.INPUT, src.getContext());
    if (var == null)
      throw new FHIRException("Rule \"" + ruleId + "\": Unknown input variable " + src.getContext() + ". Has the input parameter been defined for the map and for the parent group?");
    PropertyWithType prop = var.getProperty();

    boolean optional = false;
    boolean repeating = false;

    if (src.hasCondition()) {
      optional = true;
    }

    if (src.hasElement()) {
      Property element = prop.getBaseProperty().getChild(prop.getTypes().getType(), src.getElement());
      if (element == null)
        throw new Exception("Rule \"" + ruleId + "\": Unknown element name " + src.getElement() + "in " + src.getContext());
      if (element.getDefinition().getMin() == 0)
        optional = true;
      if (element.getDefinition().getMax().equals("*"))
        repeating = true;
      VariablesForProfiling result = vars.copy(optional, repeating);
      TypeDetails type = new TypeDetails(ExpressionNode.CollectionStatus.SINGLETON);
      for (ElementDefinition.TypeRefComponent tr : element.getDefinition().getType()) {
        if (!tr.hasCode())
          throw new Error("Rule \"" + ruleId + "\": Element has no type");
        TypeDetails.ProfiledType pt = new TypeDetails.ProfiledType(tr.getCode());
        if (tr.hasProfile())
          pt.addProfile(tr.getProfile());
        if (element.getDefinition().hasBinding())
          pt.addBinding(element.getDefinition().getBinding());
        type.addType(pt);
      }
      td.addText(prop.getPath() + "." + src.getElement());
      if (src.hasVariable())
        result.add(VariableMode.INPUT, src.getVariable(), new PropertyWithType(prop.getPath() + "." + src.getElement(), element, null, type));
      return result;
    } else {
      td.addText(prop.getPath()); // ditto!
      return vars.copy(optional, repeating);
    }
  }

  /**
   * Process source component
   *
   * @param ruleId  String value
   * @param context Transform Context
   * @param vars    Variables
   * @param src     Source Component
   * @return List of Variables
   * @throws FHIRException Error occurs in Fhir objects
   */
  @SuppressWarnings("unused")
  private List<Variables> processSource(String ruleId, TransformContext context, Variables vars, StructureMap.StructureMapGroupRuleSourceComponent src) throws FHIRException {
    List<Base> items;
    if (src.getContext().equals("@search")) {
      ExpressionNode expr = (ExpressionNode) src.getUserData(MAP_SEARCH_EXPRESSION);
      if (expr == null) {
        expr = getFhirPathEngine().parse(src.getElement());
        src.setUserData(MAP_SEARCH_EXPRESSION, expr);
      }
      String search = getFhirPathEngine().evaluateToString(vars, null, new StringType(), expr); // string is a holder of nothing to ensure that variables are processed correctly
      items = getServices().performSearch(context.getAppInfo(), search);
    } else {
      items = new ArrayList<>();
      Base b = vars.get(VariableMode.INPUT, src.getContext());
      if (b == null)
        throw new FHIRException("Unknown input variable " + src.getContext());

      if (!src.hasElement())
        items.add(b);
      else {
        getChildrenByName(b, src.getElement(), items);
        if (items.size() == 0 && src.hasDefaultValue())
          items.add(src.getDefaultValue());
      }
    }

    if (src.hasType()) {
      List<Base> remove = new ArrayList<>();
      for (Base item : items) {
        if (item != null && !isType(item, src.getType())) {
          remove.add(item);
        }
      }
      items.removeAll(remove);
    }

    if (src.hasCondition()) {
      ExpressionNode expr = (ExpressionNode) src.getUserData(MAP_WHERE_EXPRESSION);
      if (expr == null) {
        expr = getFhirPathEngine().parse(src.getCondition());
        //        fpe.check(context.appInfo, ??, ??, expr)
        src.setUserData(MAP_WHERE_EXPRESSION, expr);
      }
      List<Base> remove = new ArrayList<>();
      for (Base item : items) {
        if (!getFhirPathEngine().evaluateToBoolean(vars, null, item, expr))
          remove.add(item);
      }
      items.removeAll(remove);
    }

    if (src.hasCheck()) {
      ExpressionNode expr = (ExpressionNode) src.getUserData(MAP_WHERE_CHECK);
      if (expr == null) {
        expr = getFhirPathEngine().parse(src.getCheck());
        //        fpe.check(context.appInfo, ??, ??, expr)
        src.setUserData(MAP_WHERE_CHECK, expr);
      }
      List<Base> remove = new ArrayList<>();
      for (Base item : items) {
        if (!getFhirPathEngine().evaluateToBoolean(vars, null, item, expr))
          throw new FHIRException("Rule \"" + ruleId + "\": Check condition failed");
      }
    }


    if (src.hasListMode() && !items.isEmpty()) {
      switch (src.getListMode()) {
        case FIRST:
          Base bt = items.get(0);
          items.clear();
          items.add(bt);
          break;
        case NOTFIRST:
          if (items.size() > 0)
            items.remove(0);
          break;
        case LAST:
          bt = items.get(items.size() - 1);
          items.clear();
          items.add(bt);
          break;
        case NOTLAST:
          if (items.size() > 0)
            items.remove(items.size() - 1);
          break;
        case ONLYONE:
          if (items.size() > 1)
            throw new FHIRException("Rule \"" + ruleId + "\": Check condition failed: the collection has more than one item");
          break;
        case NULL:
      }
    }
    List<Variables> result = new ArrayList<>();
    for (Base r : items) {
      Variables v = vars.copy();
      if (src.hasVariable())
        v.add(VariableMode.INPUT, src.getVariable(), r);
      result.add(v);
    }
    return result;
  }


  /**
   * Analysis Target Component
   *
   * @param context        Batch Context
   * @param ruleId         String value
   * @param vars           Variables for Profiling
   * @param map            Structure Map
   * @param ruleTarget     Target Component
   * @param targetVariable String value
   * @param tw             Target Writer
   * @param profiles       Collection List of Structure Definitions
   * @param sliceName      String value
   * @throws Exception if error occurs with invalid argument
   */
  @SuppressWarnings("WeakerAccess")
  protected void analyseTarget(BatchContext context, String ruleId, VariablesForProfiling vars, StructureMap map, StructureMap.StructureMapGroupRuleTargetComponent ruleTarget, String targetVariable, TargetWriter tw, List<StructureDefinition> profiles, String sliceName) throws Exception {
    VariableForProfiling targetContextVariable = null;
    boolean isExtensionTransform = false;
    List<ElementDefinition> extensionReferences = null;
    if (ruleTarget.hasContext()) {
      targetContextVariable = vars.get(VariableMode.OUTPUT, ruleTarget.getContext());
      if (targetContextVariable == null)
        throw new Exception("Rule \"" + ruleId + "\": target context not known: " + ruleTarget.getContext());
      if (!ruleTarget.hasElement())
        throw new Exception("Rule \"" + ruleId + "\": Not supported yet");
    }


    TypeDetails type;
    if (ruleTarget.hasTransform()) {
      type = analyseTransform(context, map, ruleTarget, vars);
      // profiling: dest.setProperty(ruleTarget.getElement().hashCode(), ruleTarget.getElement(), v);
    } else {
      assert targetContextVariable != null;
      Property vp = targetContextVariable.getProperty().getBaseProperty().getChild(ruleTarget.getElement(), ruleTarget.getElement());
      if (vp == null)
        throw new Exception("Unknown Property " + ruleTarget.getElement() + " on " + targetContextVariable.getProperty().getPath());

      type = new TypeDetails(ExpressionNode.CollectionStatus.SINGLETON, vp.getType(ruleTarget.getElement()));
    }

    if (ruleTarget.getTransform() == StructureMap.StructureMapTransform.CREATE) {
      String s = getParamString(vars, ruleTarget.getParameter().get(0));
      if (getWorker().getResourceNames().contains(s)) {
        tw.newResource(ruleTarget.getVariable(), s);
      }
      if (!StringUtils.isBlank(context.getBaseGeneratedProfileUrl())) {
        assert type != null;
        String finalURL = context.getBaseGeneratedProfileUrl() + "/" + type.getType().substring(type.getType().lastIndexOf("/") + 1);
        ruleTarget.setUserData("profile-url", finalURL);
      }
    } else if (ruleTarget.getTransform() == StructureMap.StructureMapTransform.EXTENSION) {
      isExtensionTransform = true;
      VariableForProfiling inputElementVariable = vars.get(VariableMode.INPUT, targetVariable);
      ElementDefinition srcElementDefinition = inputElementVariable.getProperty().getBaseProperty().getDefinition();
      String extensionName = ruleTarget.getElement();
      String extensionUri = context.getBaseGeneratedProfileUrl() + extensionName + "-extension";
      assert targetContextVariable != null;
      String extContext = targetContextVariable.getProperty().getPath();
      String shortDescription = srcElementDefinition.getShort();
      String longDescription = srcElementDefinition.getDefinition();
      Integer min = srcElementDefinition.getMin();
      String max = srcElementDefinition.getMax();
      String extType = srcElementDefinition.getTypeFirstRep().getCode();
      List<StringType> contexts = new ArrayList<>();
      contexts.add(new StringType(extContext));
      FhirExtensionGenerator extensionGenerator = new FhirExtensionGenerator();
      StructureDefinition extensionStructureDef = context.getStructureDefinition(extensionUri);
      if (extensionStructureDef == null) {
        extensionStructureDef = extensionGenerator.generateExtensionStructureDefinition(extensionName, contexts, shortDescription, longDescription, min, max, extType);
        context.addStructureDefinition(extensionStructureDef);
      }
      profiles.add(extensionStructureDef);
      extensionReferences = extensionGenerator.generateExtensionElementDefinitions(true, targetContextVariable.getProperty().getPath(), extensionName, shortDescription, longDescription, min, max, extensionUri);//TODO why does ProfileUtilities add a slice when I add one but does not when I don't
    } else {
      boolean mapsSrc = false;
      for (StructureMap.StructureMapGroupRuleTargetParameterComponent parameter : ruleTarget.getParameter()) {
        Type parameterValue = parameter.getValue();
        if (parameterValue instanceof IdType && ((IdType) parameterValue).asStringValue().equals(targetVariable))
          mapsSrc = true;
      }
      if (mapsSrc) {
        if (targetContextVariable == null)
          throw new Error("Rule \"" + ruleId + "\": Attempt to assign with no context");
        tw.valueAssignment(ruleTarget.getContext(), targetContextVariable.getProperty().getPath() + "." + ruleTarget.getElement() + getTransformSuffix(ruleTarget.getTransform()));
      } else if (ruleTarget.hasContext()) {
        assert targetContextVariable != null;
        if (isSignificantElement(targetContextVariable.getProperty(), ruleTarget.getElement())) {
          String td = describeTransform(ruleTarget);
          if (td != null)
            tw.keyAssignment(ruleTarget.getContext(), targetContextVariable.getProperty().getPath() + "." + ruleTarget.getElement() + " = " + td);
        }
      }
    }
    Type fixed = generateFixedValue(ruleTarget);

    PropertyWithType prop = null;
    if (isExtensionTransform) {
      prop = addExtensionToProfile(targetContextVariable, extensionReferences, sliceName, fixed);
    } else {
      //Check if there is already a StructureDefinition with that URL. If so,
      //do not create the profile.
      String finalURL = ruleTarget.getUserString("profile-url");
      if (finalURL == null || context.getStructureDefinition(finalURL) == null) {
        prop = updateProfile(targetContextVariable, ruleTarget.getElement(), type, map, profiles, sliceName, fixed, ruleTarget);
        context.addStructureDefinition(prop.getProfileProperty().getStructure());
      }
    }

    if (ruleTarget.hasVariable() && prop != null) {
      if (ruleTarget.hasElement()) {
        vars.add(VariableMode.OUTPUT, ruleTarget.getVariable(), prop);
      } else {
        vars.add(VariableMode.OUTPUT, ruleTarget.getVariable(), prop);
      }
    }

  }

  /**
   * Processes Target Component
   *
   * @param ruleId  String value
   * @param context Transform Context
   * @param vars    Variables
   * @param map     Structure Map
   * @param group   Group component
   * @param tgt     Target component
   * @param srcVar  string Value
   * @throws FHIRException if Error occurs from
   */
  private void processTarget(String ruleId, TransformContext context, Variables vars, StructureMap map, StructureMap.StructureMapGroupComponent group, StructureMap.StructureMapGroupRuleTargetComponent tgt, String srcVar) throws FHIRException {
    Base dest = null;
    if (tgt.hasContext()) {
      dest = vars.get(VariableMode.OUTPUT, tgt.getContext());
      if (dest == null)
        throw new FHIRException("Rule \"" + ruleId + "\": target context not known: " + tgt.getContext());
      if (!tgt.hasElement())
        throw new FHIRException("Rule \"" + ruleId + "\": Not supported yet");
    }
    Base v = null;
    if (tgt.hasTransform()) {
      v = runTransform(ruleId, context, map, group, tgt, vars, dest, tgt.getElement(), srcVar);
      if (v != null && dest != null)
        v = dest.setProperty(tgt.getElement().hashCode(), tgt.getElement(), v); // reset v because some implementations may have to rewrite v when setting the value
    } else if (dest != null)
      v = dest.makeProperty(tgt.getElement().hashCode(), tgt.getElement());
    if (tgt.hasVariable() && v != null)
      vars.add(VariableMode.OUTPUT, tgt.getVariable(), v);
  }

  /**
   * Creates a parameter string from parameters and variables
   *
   * @param vars      Variables for profiling
   * @param parameter Parameter Component
   * @return String form of a parameter
   */
  @SuppressWarnings("unused")
  private String getParamString(VariablesForProfiling vars, StructureMap.StructureMapGroupRuleTargetParameterComponent parameter) {
    Type p = parameter.getValue();
    if (p == null || p instanceof IdType)
      return null;
    if (!p.hasPrimitiveValue())
      return null;
    return p.primitiveValue();
  }

  /**
   * Analyses the transform
   *
   * @param context Batch context
   * @param map     Structure Map
   * @param tgt     Target component
   * @param vars    Variables for profiling
   * @return Type Details object
   * @throws FHIRException if error occurs within Fhir code
   */
  @SuppressWarnings("unused")
  private TypeDetails analyseTransform(BatchContext context, StructureMap map, StructureMap.StructureMapGroupRuleTargetComponent tgt, VariablesForProfiling vars) throws FHIRException {
    switch (tgt.getTransform()) {
      case CREATE:
        String p = getParamString(vars, tgt.getParameter().get(0));
        return new TypeDetails(ExpressionNode.CollectionStatus.SINGLETON, p);
      case COPY:
        return getParam(vars, tgt.getParameter().get(0));
      case EVALUATE:
        ExpressionNode expr = (ExpressionNode) tgt.getUserData(MAP_EXPRESSION);
        if (expr == null) {
          expr = getFhirPathEngine().parse(getParamString(vars, tgt.getParameter().get(tgt.getParameter().size() - 1)));
          tgt.setUserData(MAP_WHERE_EXPRESSION, expr);
        }
        return getFhirPathEngine().check(vars, null, expr);

////case TRUNCATE :
////  String src = getParamString(vars, tgt.getParameter().get(0));
////  String len = getParamString(vars, tgt.getParameter().get(1));
////  if (Utilities.isInteger(len)) {
////    int l = Integer.parseInt(len);
////    if (src.length() > l)
////      src = src.substring(0, l);
////  }
////  return new StringType(src);
////case ESCAPE :
////  throw new Error("Transform "+tgt.getTransform().toCode()+" not supported yet");
////case CAST :
////  throw new Error("Transform "+tgt.getTransform().toCode()+" not supported yet");
////case APPEND :
////  throw new Error("Transform "+tgt.getTransform().toCode()+" not supported yet");
      case TRANSLATE:
        return new TypeDetails(ExpressionNode.CollectionStatus.SINGLETON, "CodeableConcept");
      case CC:
        TypeDetails.ProfiledType res = new TypeDetails.ProfiledType("CodeableConcept");
        if (tgt.getParameter().size() >= 2 && isParamId(vars, tgt.getParameter().get(1))) {
          TypeDetails td = vars.get(null, getParamId(vars, tgt.getParameter().get(1))).getProperty().getTypes();
          if (td != null && td.hasBinding())
            // todo: do we need to check that there's no implicit translation her? I don't think we do...
            res.addBinding(td.getBinding());
        }
        return new TypeDetails(ExpressionNode.CollectionStatus.SINGLETON, res);
      case C:
        return new TypeDetails(ExpressionNode.CollectionStatus.SINGLETON, "Coding");
      case QTY:
        return new TypeDetails(ExpressionNode.CollectionStatus.SINGLETON, "Quantity");
      case REFERENCE:
        VariableForProfiling vrs = vars.get(VariableMode.OUTPUT, getParamId(vars, tgt.getParameterFirstRep()));
        if (vrs == null)
          throw new FHIRException("Unable to resolve variable \"" + getParamId(vars, tgt.getParameterFirstRep()) + "\"");
        String profile = vrs.getProperty().getProfileProperty().getStructure().getUrl();
        TypeDetails td = new TypeDetails(ExpressionNode.CollectionStatus.SINGLETON);
        td.addType("Reference", profile);
        return td;
      case EXTENSION:
        return null;//TODO Figure out what to do here.
////case DATEOP :
////  throw new Error("Transform "+tgt.getTransform().toCode()+" not supported yet");
////case UUID :
////  return new IdType(UUID.randomUUID().toString());
////case POINTER :
////  Base b = getParam(vars, tgt.getParameter().get(0));
////  if (b instanceof Resource)
////    return new UriType("urn:uuid:"+((Resource) b).getId());
////  else
////    throw new FHIRException("Transform engine cannot point at an element of type "+b.fhirType());
      default:
        throw new Error("Transform Unknown or not handled yet: " + tgt.getTransform().toCode());
    }
  }

  /**
   * Runs the transform
   *
   * @param ruleId  String value
   * @param context Transform Context
   * @param map     Structure Map
   * @param group   Group component
   * @param tgt     Target component
   * @param vars    Variables
   * @param dest    Fhir Base Object
   * @param element String value
   * @param srcVar  String value
   * @return Fhir Base Object
   * @throws FHIRException if error occurs within Fhir object
   */
  private Base runTransform(String ruleId, TransformContext context, StructureMap map, StructureMap.StructureMapGroupComponent group, StructureMap.StructureMapGroupRuleTargetComponent tgt, Variables vars, Base dest, String element, String srcVar) throws FHIRException {
    try {
      switch (tgt.getTransform()) {
        case CREATE:
          String tn;
          if (tgt.getParameter().isEmpty()) {
            // we have to work out the type. First, we see if there is a single type for the target. If there is, we use that
            String[] types = dest.getTypesForProperty(element.hashCode(), element);
            if (types.length == 1 && !"*".equals(types[0]) && !types[0].equals("Resource"))
              tn = types[0];
            else if (srcVar != null) {
              tn = determineTypeFromSourceType(map, group, vars.get(VariableMode.INPUT, srcVar));
            } else
              throw new Error("Cannot determine type implicitly because there is no single input variable");
          } else
            tn = getParamStringNoNull(vars, tgt.getParameter().get(0), tgt.toString());
          Base res = getServices() != null ? getServices().createType(context.getAppInfo(), tn) : ResourceFactory.createResourceOrType(tn);
          if (res.isResource() && !res.fhirType().equals("Parameters")) {
//	        res.setIdBase(tgt.getParameter().size() > 1 ? getParamString(vars, tgt.getParameter().get(0)) : UUID.randomUUID().toString().toLowerCase());
            if (getServices() != null)
              res = getServices().createResource(context.getAppInfo(), res);
          }
          if (tgt.hasUserData("profile"))
            res.setUserData("profile", tgt.getUserData("profile"));
          return res;
        case COPY:
          return getParam(vars, tgt.getParameter().get(0));
        case EVALUATE:
          ExpressionNode expr = (ExpressionNode) tgt.getUserData(MAP_EXPRESSION);
          if (expr == null) {
            expr = getFhirPathEngine().parse(getParamStringNoNull(vars, tgt.getParameter().get(1), tgt.toString()));
            tgt.setUserData(MAP_WHERE_EXPRESSION, expr);
          }
          List<Base> v = getFhirPathEngine().evaluate(vars, null, tgt.getParameter().size() == 2 ? getParam(vars, tgt.getParameter().get(0)) : new BooleanType(false), expr);
          if (v.size() == 0)
            return null;
          else if (v.size() != 1)
            throw new FHIRException("Rule \"" + ruleId + "\": Evaluation of " + expr.toString() + " returned " + Integer.toString(v.size()) + " objects");
          else
            return v.get(0);

        case TRUNCATE:
          String src = getParamString(vars, tgt.getParameter().get(0));
          String len = getParamStringNoNull(vars, tgt.getParameter().get(1), tgt.toString());
          if (Utilities.isInteger(len)) {
            int l = Integer.parseInt(len);
            if (src.length() > l)
              src = src.substring(0, l);
          }
          return new StringType(src);
        case ESCAPE:
          throw new Error("Rule \"" + ruleId + "\": Transform " + tgt.getTransform().toCode() + " not supported yet");
        case CAST:
          throw new Error("Rule \"" + ruleId + "\": Transform " + tgt.getTransform().toCode() + " not supported yet");
        case APPEND:
          throw new Error("Rule \"" + ruleId + "\": Transform " + tgt.getTransform().toCode() + " not supported yet");
        case TRANSLATE:
          return transformationEngine.translate(context, map, vars, tgt.getParameter());
        case REFERENCE:
          Base b = getParam(vars, tgt.getParameter().get(0));
          if (b == null)
            throw new FHIRException("Rule \"" + ruleId + "\": Unable to find parameter " + ((IdType) tgt.getParameter().get(0).getValue()).asStringValue());
          if (!b.isResource())
            throw new FHIRException("Rule \"" + ruleId + "\": Transform engine cannot point at an element of type " + b.fhirType());
          else {
            String id = b.getIdBase();
            if (id == null) {
              id = UUID.randomUUID().toString().toLowerCase();
              b.setIdBase(id);
            }
            return new Reference().setReference(b.fhirType() + "/" + id);
          }
        case DATEOP:
          throw new Error("Rule \"" + ruleId + "\": Transform " + tgt.getTransform().toCode() + " not supported yet");
        case UUID:
          return new IdType(UUID.randomUUID().toString());
        case POINTER:
          b = getParam(vars, tgt.getParameter().get(0));
          if (b instanceof Resource)
            return new UriType("urn:uuid:" + ((Resource) b).getId());
          else
            throw new FHIRException("Rule \"" + ruleId + "\": Transform engine cannot point at an element of type " + b.fhirType());
        case CC:
          CodeableConcept cc = new CodeableConcept();
          cc.addCoding(buildCoding(getParamStringNoNull(vars, tgt.getParameter().get(0), tgt.toString()), getParamStringNoNull(vars, tgt.getParameter().get(1), tgt.toString())));
          return cc;
        case C:
          return buildCoding(getParamStringNoNull(vars, tgt.getParameter().get(0), tgt.toString()), getParamStringNoNull(vars, tgt.getParameter().get(1), tgt.toString()));
        default:
          throw new Error("Rule \"" + ruleId + "\": Transform Unknown: " + tgt.getTransform().toCode());
      }
    } catch (Exception e) {
      throw new FHIRException("Exception executing transform " + tgt.toString() + " on Rule \"" + ruleId + "\": " + e.getMessage(), e);
    }
  }


  /**
   * get a string representation of a parameter
   *
   * @param vars      Variables for Profiling
   * @param parameter Parameter Component
   * @return String value of a primitive type
   */
  @SuppressWarnings("unused")
  private String getParamId(VariablesForProfiling vars, StructureMap.StructureMapGroupRuleTargetParameterComponent parameter) {
    Type p = parameter.getValue();
    if (p == null || !(p instanceof IdType))
      return null;
    return p.primitiveValue();
  }

  /**
   * verify if an object is a parameter Id
   *
   * @param vars      Variables for profiling
   * @param parameter Parameter Component
   * @return if parameter has id type
   */
  private boolean isParamId(VariablesForProfiling vars, StructureMap.StructureMapGroupRuleTargetParameterComponent parameter) {
    Type p = parameter.getValue();
    return p != null && p instanceof IdType && vars.get(null, p.primitiveValue()) != null;
  }

  /**
   * gets type details from a parameter
   *
   * @param vars      Variables for Profiling
   * @param parameter Parameter Component
   * @return types of the parameter
   * @throws DefinitionException if Variable is not present in vars
   */
  private TypeDetails getParam(VariablesForProfiling vars, StructureMap.StructureMapGroupRuleTargetParameterComponent parameter) throws DefinitionException {
    Type p = parameter.getValue();
    if (!(p instanceof IdType))
      return new TypeDetails(ExpressionNode.CollectionStatus.SINGLETON, "http://hl7.org/fhir/StructureDefinition/" + p.fhirType());
    else {
      String n = ((IdType) p).asStringValue();
      VariableForProfiling b = vars.get(VariableMode.INPUT, n);
      if (b == null)
        b = vars.get(VariableMode.OUTPUT, n);
      if (b == null)
        throw new DefinitionException("Variable " + n + " not found (" + vars.summary() + ")");
      return b.getProperty().getTypes();
    }
  }

  /**
   * gets the relative transformation suffix
   *
   * @param transform Transform
   * @return String value of the suffix
   */
  private String getTransformSuffix(StructureMap.StructureMapTransform transform) {
    switch (transform) {
      case COPY:
        return "";
      case TRUNCATE:
        return " (truncated)";
      //case ESCAPE:
      //case CAST:
      //case APPEND:
      case TRANSLATE:
        return " (translated)";
      //case DATEOP,
      //case UUID,
      //case POINTER,
      //case EVALUATE,
      case CC:
        return " (--> CodeableConcept)";
      case C:
        return " (--> Coding)";
      case QTY:
        return " (--> Quantity)";
      //case ID,
      //case CP,
      default:
        return " {??)";
    }
  }

  /**
   * returns true if property and element are both significant by relative means
   *
   * @param property Property with Type
   * @param element  String value
   * @return true if significant
   */
  private boolean isSignificantElement(PropertyWithType property, String element) {
    if ("Observation".equals(property.getPath()))
      return "code".equals(element);
    else
      return "Bundle".equals(property.getPath()) && "type".equals(element);
  }

  /**
   * get transform based on String input
   *
   * @param ruleTarget Target component
   * @return Transform enum object
   * @throws FHIRException if called methods throw and error
   */
  private String describeTransform(StructureMap.StructureMapGroupRuleTargetComponent ruleTarget) throws FHIRException {
    switch (ruleTarget.getTransform()) {
      case COPY:
        return null;
      case TRUNCATE:
        return null;
      //case ESCAPE:
      //case CAST:
      //case APPEND:
      case TRANSLATE:
        return null;
      //case DATEOP,
      //case UUID,
      //case POINTER,
      //case EVALUATE,
      case CC:
        return describeTransformCCorC(ruleTarget);
      case C:
        return describeTransformCCorC(ruleTarget);
      case QTY:
        return null;
      //case ID,
      //case CP,
      default:
        return null;
    }
  }

  /**
   * describes are CodeableConcept or Coding transform
   *
   * @param tgt Target Component
   * @return string result
   * @throws FHIRException if certain parameters within target are null or invalid
   */
  @SuppressWarnings("rawtypes")
  private String describeTransformCCorC(StructureMap.StructureMapGroupRuleTargetComponent tgt) throws FHIRException {
    if (tgt.getParameter().size() < 2)
      return null;
    Type p1 = tgt.getParameter().get(0).getValue();
    Type p2 = tgt.getParameter().get(1).getValue();
    if (p1 instanceof IdType || p2 instanceof IdType)
      return null;
    if (!(p1 instanceof PrimitiveType) || !(p2 instanceof PrimitiveType))
      return null;
    String uri = ((PrimitiveType) p1).asStringValue();
    String code = ((PrimitiveType) p2).asStringValue();
    if (Utilities.noString(uri))
      throw new FHIRException("Describe Transform, but the uri is blank");
    if (Utilities.noString(code))
      throw new FHIRException("Describe Transform, but the code is blank");
    Coding c = buildCoding(uri, code);
    return NarrativeGenerator.describeSystem(c.getSystem()) + "#" + c.getCode() + (c.hasDisplay() ? "(" + c.getDisplay() + ")" : "");
  }

  /**
   * adds Extension to the profile
   *
   * @param var         Variables for Profiling
   * @param definitions List of Element definitions
   * @param sliceName   String value
   * @param fixed       Fhir model Type
   * @return Property with Type instantiated when validated
   * @throws FHIRException if definitions has too many or too little entries
   */
  @SuppressWarnings("unused")
  private PropertyWithType addExtensionToProfile(VariableForProfiling var, List<ElementDefinition> definitions, String sliceName, Type fixed) throws FHIRException {
    StructureDefinition sd = var.getProperty().getProfileProperty().getStructure();
    ElementDefinition extensionReference;
    if (definitions.size() == 0 || definitions.size() > 2) {
      throw new FHIRException("Error handling extensions to the type");
    } else if (definitions.size() == 1) {
      sd.getDifferential().addElement(extensionReference = definitions.get(0));
    } else {
      sd.getDifferential().addElement(definitions.get(0));
      sd.getDifferential().addElement(extensionReference = definitions.get(1));
    }
    return new PropertyWithType(var.getProperty().getPath() + ".extension", null, new Property(getWorker(), extensionReference, sd), null);
  }

  /**
   * generates a fixed value based on transform, can return null
   *
   * @param tgt Target Component
   * @return Fhir model Type
   */
  private Type generateFixedValue(StructureMap.StructureMapGroupRuleTargetComponent tgt) {
    if (!allParametersFixed(tgt))
      return null;
    if (!tgt.hasTransform())
      return null;
    switch (tgt.getTransform()) {
      case COPY:
        return tgt.getParameter().get(0).getValue();
      case TRUNCATE:
        return null;
      //case ESCAPE:
      //case CAST:
      //case APPEND:
      case TRANSLATE:
        return null;
      //case DATEOP,
      //case UUID,
      //case POINTER,
      //case EVALUATE,
      case CC:
        CodeableConcept cc = new CodeableConcept();
        cc.addCoding(buildCoding(tgt.getParameter().get(0).getValue(), tgt.getParameter().get(1).getValue()));
        return cc;
      case C:
        return buildCoding(tgt.getParameter().get(0).getValue(), tgt.getParameter().get(1).getValue());
      case QTY:
        return null;
      //case ID,
      //case CP,
      default:
        return null;
    }
  }

  /**
   * returns true if all parameters are a fixed type
   *
   * @param tgt Target component
   * @return true if all are fixed.
   */
  private boolean allParametersFixed(StructureMap.StructureMapGroupRuleTargetComponent tgt) {
    for (StructureMap.StructureMapGroupRuleTargetParameterComponent p : tgt.getParameter()) {
      Type pr = p.getValue();
      if (pr instanceof IdType)
        return false;
    }
    return true;
  }

  /**
   * Updates the Fhir Profile
   *
   * @param var       Variable for Profiling
   * @param element   String value
   * @param type      Type Details
   * @param map       Structure Map
   * @param profiles  List of Structure Definitions
   * @param sliceName String value
   * @param fixed     Fhir Model Type
   * @param tgt       Target Component
   * @return Property with Type
   * @throws FHIRException if the base property in var doesn't have a corresponding child with element and the types within the property in var
   */
  @SuppressWarnings({"UnnecessaryLocalVariable", "unused"})
  private PropertyWithType updateProfile(VariableForProfiling var, String element, TypeDetails type, StructureMap map, List<StructureDefinition> profiles, String sliceName, Type fixed, StructureMap.StructureMapGroupRuleTargetComponent tgt) throws FHIRException {
    if (var == null) {
      assert (Utilities.noString(element));
      // 1. start the new structure definition
      StructureDefinition sdn = getWorker().fetchResource(StructureDefinition.class, type.getType());
      if (sdn == null)
        throw new FHIRException("Unable to find definition for " + type.getType());
      ElementDefinition edn = sdn.getSnapshot().getElementFirstRep();
      PropertyWithType pn = createProfile(map, profiles, new PropertyWithType(sdn.getId(), new Property(getWorker(), edn, sdn), null, type), sliceName, tgt);

//      // 2. hook it into the base bundle
//      if (type.getType().startsWith("http://hl7.org/fhir/StructureDefinition/") && worker.getResourceNames().contains(type.getType().substring(40))) {
//        StructureDefinition sd = var.getProperty().profileProperty.getStructure();
//        ElementDefinition ed = sd.getDifferential().addElement();
//        ed.setPath("Bundle.entry");
//        ed.setName(sliceName);
//        ed.setMax("1"); // well, it is for now...
//        ed = sd.getDifferential().addElement();
//        ed.setPath("Bundle.entry.fullUrl");
//        ed.setMin(1);
//        ed = sd.getDifferential().addElement();
//        ed.setPath("Bundle.entry.resource");
//        ed.setMin(1);
//        ed.addType().setCode(pn.getProfileProperty().getStructure().getType()).setProfile(pn.getProfileProperty().getStructure().getUrl());
//      }
      return pn;
    } else {
      assert (!Utilities.noString(element));
      Property pvb = var.getProperty().getBaseProperty();
      Property pvd = var.getProperty().getProfileProperty();
      Property pc = pvb.getChild(element, var.getProperty().getTypes());
      if (pc == null)
        throw new DefinitionException("Unable to find a definition for " + pvb.getDefinition().getPath() + "." + element);

      // the profile structure definition (derived)
      StructureDefinition sd = var.getProperty().getProfileProperty().getStructure();
      ElementDefinition edNew = sd.getDifferential().addElement();
      edNew.setPath(var.getProperty().getProfileProperty().getDefinition().getPath() + "." + pc.getName());
      edNew.setUserData("slice-name", sliceName);
      edNew.setFixed(fixed);
      for (TypeDetails.ProfiledType pt : type.getProfiledTypes()) {
        if (pt.hasBindings())
          edNew.setBinding(pt.getBindings().get(0));
        if (pt.getUri().startsWith("http://hl7.org/fhir/StructureDefinition/")) {
          String t = pt.getUri().substring(40);
          t = checkType(t, pc, pt.getProfiles());
          if (t != null) {
            if (pt.hasProfiles()) {
              for (String p : pt.getProfiles())
                if (t.equals("Reference"))
                  edNew.addType().setCode(t).setTargetProfile(p);
                else
                  edNew.addType().setCode(t).setProfile(p);
            } else
              edNew.addType().setCode(t);
          }
        }
      }

      return new PropertyWithType(var.getProperty().getPath() + "." + element, pc, new Property(getWorker(), edNew, sd), type);
    }
  }

  /**
   * checks type of the property based on the profiles given
   *
   * @param t        target type
   * @param pvb      Property
   * @param profiles List of strings
   * @return String  of the type's code
   * @throws FHIRException if t isn't a compatible with the types for the paths of the definitions in pvb
   */
  private String checkType(String t, Property pvb, List<String> profiles) throws FHIRException {
    if (pvb.getDefinition().getType().size() == 1 && isCompatibleType(t, pvb.getDefinition().getType().get(0).getCode()) && profilesMatch(profiles, pvb.getDefinition().getType().get(0).getProfile()))
      return null;
    for (ElementDefinition.TypeRefComponent tr : pvb.getDefinition().getType()) {
      if (isCompatibleType(t, tr.getCode()))
        return tr.getCode(); // note what is returned - the base type, not the inferred mapping type
    }
    throw new FHIRException("The type " + t + " is not compatible with the allowed types for " + pvb.getDefinition().getPath());
  }

  /**
   * tests to see if the types are compatible
   *
   * @param t String
   * @param code Sting
   * @return true if compatible
   */
  private boolean isCompatibleType(String t, String code) {
    if (t.equals(code))
      return true;
    if (t.equals("string")) {
      StructureDefinition sd = getWorker().fetchResource(StructureDefinition.class, "http://hl7.org/fhir/StructureDefinition/" + code);
      if (sd != null && sd.getBaseDefinition().equals("http://hl7.org/fhir/StructureDefinition/string"))
        return true;
    }
    return false;
  }

  /**
   * Tests to see if the string is contained in the list at the first element
   *
   * @param profiles List of strings
   * @param profile  String value
   * @return true if list contains the string in the first entry
   */
  private boolean profilesMatch(List<String> profiles, String profile) {
    return profiles == null || profiles.size() == 0 || (profiles.size() == 1 && profiles.get(0).equals(profile));
  }

  /**
   * @param map    Structure Map
   * @param source Group Component
   * @param base   Fhir Model Base
   * @return String value of the determined type
   * @throws FHIRException if map, source or base contain invalid values
   */
  private String determineTypeFromSourceType(StructureMap map, StructureMap.StructureMapGroupComponent source, Base base) throws FHIRException {
    String type = base.fhirType();
    String kn = "type^" + type;
    if (source.hasUserData(kn))
      return source.getUserString(kn);

    ResolvedGroup res = new ResolvedGroup();
    res.targetMap = null;
    res.target = null;
    for (StructureMap.StructureMapGroupComponent grp : map.getGroup()) {
      if (matchesByType(map, grp, type)) {
        if (res.targetMap == null) {
          res.targetMap = map;
          res.target = grp;
        } else
          throw new FHIRException("Multiple possible matches looking for default rule for '" + type + "'");
      }
    }
    if (res.targetMap != null) {
      String result = getActualType(res.targetMap, res.target.getInput().get(1).getType());
      source.setUserData(kn, result);
      return result;
    }

    for (UriType imp : map.getImport()) {
      List<StructureMap> impMapList = findMatchingMaps(imp.getValue());
      if (impMapList.size() == 0)
        throw new FHIRException("Unable to find map(s) for " + imp.getValue());
      for (StructureMap impMap : impMapList) {
        if (!impMap.getUrl().equals(map.getUrl())) {
          for (StructureMap.StructureMapGroupComponent grp : impMap.getGroup()) {
            if (matchesByType(impMap, grp, type)) {
              if (res.targetMap == null) {
                res.targetMap = impMap;
                res.target = grp;
              } else
                throw new FHIRException("Multiple possible matches for default rule for '" + type + "' in " + res.targetMap.getUrl() + " (" + res.target.getName() + ") and " + impMap.getUrl() + " (" + grp.getName() + ")");
            }
          }
        }
      }
    }
    if (res.target == null)
      throw new FHIRException("No matches found for default rule for '" + type + "' from " + map.getUrl());
    String result = getActualType(res.targetMap, res.target.getInput().get(1).getType()); // should be .getType, but R2...
    source.setUserData(kn, result);
    return result;
  }

  /**
   * gets actual type
   *
   * @param map        Structure Map
   * @param statedType string value
   * @return statedType if valid
   * @throws FHIRException if worker doesn't have a Structure Definition based on the url
   */
  private String getActualType(StructureMap map, String statedType) throws FHIRException {
    // check the aliases
    for (StructureMap.StructureMapStructureComponent imp : map.getStructure()) {
      if (imp.hasAlias() && statedType.equals(imp.getAlias())) {
        StructureDefinition sd = getWorker().fetchResource(StructureDefinition.class, imp.getUrl());
        if (sd == null)
          throw new FHIRException("Unable to resolve structure " + imp.getUrl());
        return sd.getId(); // should be sd.getType(), but R2...
      }
    }
    return statedType;
  }

  /**
   * checks if the type is matched to the group's actual type
   *
   * @param map  Structure Map
   * @param grp  Group Component
   * @param type String value
   * @return true if matched
   * @throws FHIRException if error occurs within Fhir object calls
   */
  private boolean matchesByType(StructureMap map, StructureMap.StructureMapGroupComponent grp, String type) throws FHIRException {
    return grp.getTypeMode() == StructureMap.StructureMapGroupTypeMode.TYPEANDTYPES &&
      grp.getInput().size() == 2 && grp.getInput().get(0).getMode() == StructureMap.StructureMapInputMode.SOURCE &&
      grp.getInput().get(1).getMode() == StructureMap.StructureMapInputMode.TARGET &&
      matchesType(map, type, grp.getInput().get(0).getType());
  }

  /**
   * checks if the source and target type are matched to the group's actual type
   *
   * @param map Structure map
   * @param grp Structure Group
   * @param srcType Source type
   * @param tgtType Target type
   * @return true if type matches
   * @throws FHIRException if error occurs within Fhir object calls
   */
  private boolean matchesByType(StructureMap map, StructureMap.StructureMapGroupComponent grp, String srcType, String tgtType) throws FHIRException {
    return grp.getTypeMode() != StructureMap.StructureMapGroupTypeMode.NONE && grp.getInput().size() == 2 &&
      grp.getInput().get(0).getMode() == StructureMap.StructureMapInputMode.SOURCE &&
      grp.getInput().get(1).getMode() == StructureMap.StructureMapInputMode.TARGET &&
      grp.getInput().get(0).hasType() && grp.getInput().get(1).hasType() &&
      matchesType(map, srcType, grp.getInput().get(0).getType()) &&
      matchesType(map, tgtType, grp.getInput().get(1).getType());
  }

  /**
   * matches type with actual type
   *
   * @param map        Structure Map
   * @param actualType String value
   * @param statedType String value
   * @return true if matched
   * @throws FHIRException if error occurs within Fhir object calls
   */
  private boolean matchesType(StructureMap map, String actualType, String statedType) throws FHIRException {
    // check the aliases
    for (StructureMap.StructureMapStructureComponent imp : map.getStructure()) {
      if (imp.hasAlias() && statedType.equals(imp.getAlias())) {
        StructureDefinition sd = getWorker().fetchResource(StructureDefinition.class, imp.getUrl());
        if (sd != null)
          statedType = sd.getType();
        break;
      }
    }

    return actualType.equals(statedType);
  }

  /**
   * Given an item, return all the children that conform to the pattern described in name
   * <p>
   * Possible patterns:
   * - a simple name (which may be the base of a name with [] e.g. value[x])
   * - a name with a type replacement e.g. valueCodeableConcept
   * - * which means all children
   * - ** which means all descendants
   *
   * @param item   Fhir Model Base
   * @param name   String value
   * @param result List of Fhir Model Bases
   * @throws FHIRException If Fhir Object errors occur
   */
  private void getChildrenByName(Base item, String name, List<Base> result) throws FHIRException {
    for (Base v : item.listChildrenByName(name, true))
      if (v != null)
        result.add(v);
  }

  /**
   * Tests to see if item is stated type
   *
   * @param item Fhir Model Base
   * @param type String value
   * @return true if the fhir type of item is equal to type
   */
  private boolean isType(Base item, String type) {
    return type.equals(item.fhirType());
  }

}