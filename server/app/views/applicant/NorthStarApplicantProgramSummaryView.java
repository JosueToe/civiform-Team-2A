package views.applicant;

import auth.CiviFormProfile;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import controllers.AssetsFinder;
import controllers.FlashKey;
import controllers.LanguageUtils;
import controllers.applicant.ApplicantRoutes;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import modules.ThymeleafModule;
import org.thymeleaf.TemplateEngine;
import play.i18n.Messages;
import play.mvc.Http.Request;
import services.AlertSettings;
import services.DeploymentType;
import services.applicant.ApplicantPersonalInfo;
import services.applicant.Block;
import services.settings.SettingsManifest;
import views.NorthStarBaseView;

/** Renders a list of sections in the form with their status. */
public final class NorthStarApplicantProgramSummaryView extends NorthStarBaseView {

  @Inject
  NorthStarApplicantProgramSummaryView(
      TemplateEngine templateEngine,
      ThymeleafModule.PlayThymeleafContextFactory playThymeleafContextFactory,
      AssetsFinder assetsFinder,
      ApplicantRoutes applicantRoutes,
      SettingsManifest settingsManifest,
      LanguageUtils languageUtils,
      DeploymentType deploymentType) {
    super(
        templateEngine,
        playThymeleafContextFactory,
        assetsFinder,
        applicantRoutes,
        settingsManifest,
        languageUtils,
        deploymentType);
  }

  public String render(Request request, Params params) {
    ThymeleafModule.PlayThymeleafContext context =
        createThymeleafContext(
            request,
            Optional.of(params.applicantId()),
            Optional.of(params.profile()),
            params.applicantPersonalInfo(),
            params.messages());
    context.setVariable("programTitle", params.programTitle());
    context.setVariable("blocks", params.blocks());
    context.setVariable("blockEditUrlMap", blockEditUrlMap(params));
    context.setVariable("continueUrl", getContinueUrl(params));
    context.setVariable(
        "hasCompletedAllBlocks", params.completedBlockCount() == params.totalBlockCount());
    context.setVariable("submitUrl", getSubmitUrl(params));

    // Toasts
    context.setVariable("alertBannerMessage", params.alertBannerMessage());
    context.setVariable("successBannerMessage", params.successBannerMessage());
    context.setVariable("notEligibleBannerMessage", params.notEligibleBannerMessage());
    context.setVariable("errorBannerMessage", request.flash().get(FlashKey.ERROR));

    // Modals
    Optional<String> duplicateFlashValue = request.flash().get(FlashKey.DUPLICATE_SUBMISSION);
    context.setVariable("duplicateSubmission", duplicateFlashValue);
    context.setVariable("exitHref", "/");

    // Eligibility Alerts
    context.setVariable("eligibilityAlertSettings", params.eligibilityAlertSettings());

    // Login modal
    Optional<String> redirectedFromProgramSlug =
        request.flash().get(FlashKey.REDIRECTED_FROM_PROGRAM_SLUG);
    context.setVariable("redirectedFromProgramSlug", redirectedFromProgramSlug);
    if (redirectedFromProgramSlug.isPresent()) {
      String postLoginRedirect =
          controllers.applicant.routes.ApplicantProgramsController.show(
                  request.flash().get(FlashKey.REDIRECTED_FROM_PROGRAM_SLUG).get())
              .url();
      context.setVariable("slugBypassUrl", postLoginRedirect);
      context.setVariable(
          "slugLoginUrl",
          controllers.routes.LoginController.applicantLogin(Optional.of(postLoginRedirect)).url());
      context.setVariable(
          "authProviderName",
          // The applicant portal name should always be set (there is a
          // default setting as well).
          settingsManifest.getApplicantPortalName(request).get());
    }

    return templateEngine.process("applicant/ApplicantProgramSummaryTemplate", context);
  }

  // Returns a map of block ids to edit urls.
  private Map<String, String> blockEditUrlMap(Params params) {
    return params.blocks().stream()
        .collect(Collectors.toMap(value -> value.getId(), value -> getBlockEditUrl(params, value)));
  }

  private String getBlockEditUrl(Params params, Block block) {
    if (block.isAnsweredWithoutErrors()) {
      return applicantRoutes
          .blockReview(
              params.profile(),
              params.applicantId(),
              params.programId(),
              block.getId(),
              Optional.empty())
          .url();
    } else {
      return applicantRoutes
          .blockEdit(
              params.profile(),
              params.applicantId(),
              params.programId(),
              block.getId(),
              Optional.empty())
          .url();
    }
  }

  private String getContinueUrl(Params params) {
    return applicantRoutes.edit(params.profile(), params.applicantId(), params.programId()).url();
  }

  private String getSubmitUrl(Params params) {
    return applicantRoutes.submit(params.profile(), params.applicantId(), params.programId()).url();
  }

  @AutoValue
  public abstract static class Params {

    public static Builder builder() {
      return new AutoValue_NorthStarApplicantProgramSummaryView_Params.Builder();
    }

    abstract String programTitle();

    abstract long applicantId();

    abstract ApplicantPersonalInfo applicantPersonalInfo();

    abstract ImmutableList<Block> blocks();

    abstract int completedBlockCount();

    abstract CiviFormProfile profile();

    abstract long programId();

    abstract int totalBlockCount();

    abstract Messages messages();

    abstract Optional<String> alertBannerMessage();

    abstract Optional<String> successBannerMessage();

    abstract Optional<String> notEligibleBannerMessage();

    abstract AlertSettings eligibilityAlertSettings();

    @AutoValue.Builder
    public abstract static class Builder {

      public abstract Builder setProgramTitle(String programTitle);

      public abstract Builder setApplicantId(long applicantId);

      public abstract Builder setApplicantPersonalInfo(ApplicantPersonalInfo personalInfo);

      public abstract Builder setBlocks(ImmutableList<Block> blocks);

      public abstract Builder setCompletedBlockCount(int completedBlockCount);

      public abstract Builder setProfile(CiviFormProfile profile);

      public abstract Builder setProgramId(long programId);

      public abstract Builder setTotalBlockCount(int totalBlockCount);

      public abstract Builder setMessages(Messages messages);

      public abstract Builder setAlertBannerMessage(Optional<String> alertBannerMessage);

      public abstract Builder setSuccessBannerMessage(Optional<String> successBannerMessage);

      public abstract Builder setNotEligibleBannerMessage(
          Optional<String> notEligibleBannerMessage);

      public abstract Builder setEligibilityAlertSettings(AlertSettings eligibilityAlertSettings);

      public abstract Params build();
    }
  }
}
