package com.ctrip.framework.apollo.portal.controller;

import com.ctrip.framework.apollo.common.dto.GrayReleaseRuleDTO;
import com.ctrip.framework.apollo.common.dto.NamespaceDTO;
import com.ctrip.framework.apollo.common.dto.ReleaseDTO;
import com.ctrip.framework.apollo.core.enums.Env;
import com.ctrip.framework.apollo.portal.auth.PermissionValidator;
import com.ctrip.framework.apollo.portal.entity.model.NamespaceReleaseModel;
import com.ctrip.framework.apollo.portal.entity.vo.NamespaceVO;
import com.ctrip.framework.apollo.portal.service.NamespaceBranchService;
import com.ctrip.framework.apollo.portal.service.ReleaseService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NamespaceBranchController {

  @Autowired
  private PermissionValidator permissionValidator;
  @Autowired
  private ReleaseService releaseService;
  @Autowired
  private NamespaceBranchService namespaceBranchService;

  @RequestMapping("/apps/{appId}/envs/{env}/clusters/{clusterName}/namespaces/{namespaceName}/branches")
  public NamespaceVO findBranch(@PathVariable String appId,
                                @PathVariable String env,
                                @PathVariable String clusterName,
                                @PathVariable String namespaceName) {
    return namespaceBranchService.findBranch(appId, Env.valueOf(env), clusterName, namespaceName);
  }

  @PreAuthorize(value = "@permissionValidator.hasModifyNamespacePermission(#appId, #namespaceName)")
  @RequestMapping(value = "/apps/{appId}/envs/{env}/clusters/{clusterName}/namespaces/{namespaceName}/branches", method = RequestMethod.POST)
  public NamespaceDTO createBranch(@PathVariable String appId,
                                   @PathVariable String env,
                                   @PathVariable String clusterName,
                                   @PathVariable String namespaceName) {

    return namespaceBranchService.createBranch(appId, Env.valueOf(env), clusterName, namespaceName);
  }

  @RequestMapping(value = "/apps/{appId}/envs/{env}/clusters/{clusterName}/namespaces/{namespaceName}/branches/{branchName}", method = RequestMethod.DELETE)
  public void deleteBranch(@PathVariable String appId,
                           @PathVariable String env,
                           @PathVariable String clusterName,
                           @PathVariable String namespaceName,
                           @PathVariable String branchName) {

    boolean canDelete = permissionValidator.hasReleaseNamespacePermission(appId, namespaceName) ||
                        (permissionValidator.hasModifyNamespacePermission(appId, namespaceName) &&
                         releaseService.loadLatestRelease(appId, Env.valueOf(env), branchName, namespaceName) == null);


    if (!canDelete) {
      throw new AccessDeniedException("Forbidden operation. "
                                      + "Caused by: 1.you don't have release permission "
                                      + "or 2. you don't have modification permission "
                                      + "or 3. you have modification permission but branch has been released");
    }

    namespaceBranchService.deleteBranch(appId, Env.valueOf(env), clusterName, namespaceName, branchName);

  }



  @PreAuthorize(value = "@permissionValidator.hasReleaseNamespacePermission(#appId, #namespaceName)")
  @RequestMapping(value = "/apps/{appId}/envs/{env}/clusters/{clusterName}/namespaces/{namespaceName}/branches/{branchName}/merge", method = RequestMethod.POST)
  public ReleaseDTO merge(@PathVariable String appId, @PathVariable String env,
                          @PathVariable String clusterName, @PathVariable String namespaceName,
                          @PathVariable String branchName, @RequestParam(value = "deleteBranch", defaultValue = "true") boolean deleteBranch,
                          @RequestBody NamespaceReleaseModel model) {

    return namespaceBranchService.merge(appId, Env.valueOf(env), clusterName, namespaceName, branchName,
                                                             model.getReleaseTitle(), model.getReleaseComment(), deleteBranch);

  }


  @RequestMapping(value = "/apps/{appId}/envs/{env}/clusters/{clusterName}/namespaces/{namespaceName}/branches/{branchName}/rules", method = RequestMethod.GET)
  public GrayReleaseRuleDTO getBranchGrayRules(@PathVariable String appId, @PathVariable String env,
                                                     @PathVariable String clusterName,
                                                     @PathVariable String namespaceName,
                                                     @PathVariable String branchName) {

    return namespaceBranchService.findBranchGrayRules(appId, Env.valueOf(env), clusterName, namespaceName, branchName);
  }


  @PreAuthorize(value = "@permissionValidator.hasOperateNamespacePermission(#appId, #namespaceName)")
  @RequestMapping(value = "/apps/{appId}/envs/{env}/clusters/{clusterName}/namespaces/{namespaceName}/branches/{branchName}/rules", method = RequestMethod.PUT)
  public void updateBranchRules(@PathVariable String appId, @PathVariable String env,
                                @PathVariable String clusterName, @PathVariable String namespaceName,
                                @PathVariable String branchName, @RequestBody GrayReleaseRuleDTO rules) {

    namespaceBranchService
        .updateBranchGrayRules(appId, Env.valueOf(env), clusterName, namespaceName, branchName, rules);

  }

}