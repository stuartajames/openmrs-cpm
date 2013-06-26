package org.openmrs.module.cpm.web.controller;

import org.openmrs.ConceptClass;
import org.openmrs.ConceptDatatype;
import org.openmrs.api.APIAuthenticationException;
import org.openmrs.api.context.Context;
import org.openmrs.module.cpm.*;
import org.openmrs.module.cpm.api.ProposedConceptService;
import org.openmrs.module.cpm.web.dto.ProposedConceptDto;
import org.openmrs.module.cpm.web.dto.SubmissionDto;
import org.openmrs.module.cpm.web.dto.SubmissionResponseDto;
import org.openmrs.module.cpm.web.dto.SubmissionStatusDto;
import org.openmrs.module.cpm.web.dto.concept.DescriptionDto;
import org.openmrs.module.cpm.web.dto.concept.NameDto;
import org.openmrs.module.cpm.web.dto.concept.NumericDto;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Controller
public class DictionaryManagerController {

	//
	// Proposer-Reviewer webservice endpoints
	//

    @RequestMapping(value = "/cpm/dictionarymanager/proposals", method = RequestMethod.POST)
    public @ResponseBody SubmissionResponseDto submitProposal(@RequestBody final SubmissionDto incomingProposal) throws IOException {

		final ProposedConceptService service = Context.getService(ProposedConceptService.class);
		final ProposedConceptResponsePackage proposedConceptResponsePackage = new ProposedConceptResponsePackage();
		proposedConceptResponsePackage.setName(incomingProposal.getName());
		proposedConceptResponsePackage.setEmail(incomingProposal.getEmail());
		proposedConceptResponsePackage.setDescription(incomingProposal.getDescription());
		proposedConceptResponsePackage.setProposedConceptPackageUuid("is-this-really-needed?");

		if (incomingProposal.getConcepts() != null) {
			for (ProposedConceptDto concept : incomingProposal.getConcepts()) {
				ProposedConceptResponse response = new ProposedConceptResponse();

				List<ProposedConceptResponseName> names = new ArrayList<ProposedConceptResponseName>();
				for (NameDto nameDto: concept.getNames()) {
					ProposedConceptResponseName name = new ProposedConceptResponseName();
					name.setName(nameDto.getName());
					name.setType(nameDto.getType());
					name.setLocale(new Locale(nameDto.getLocale()));
					names.add(name);
				}
				response.setNames(names);

				List<ProposedConceptResponseDescription> descriptions = new ArrayList<ProposedConceptResponseDescription>();
				for (DescriptionDto descriptionDto: concept.getDescriptions()) {
					ProposedConceptResponseDescription description = new ProposedConceptResponseDescription();
					description.setDescription(descriptionDto.getDescription());
					description.setLocale(new Locale(descriptionDto.getLocale()));
					descriptions.add(description);
				}
				response.setDescriptions(descriptions);

				response.setProposedConceptUuid(concept.getUuid());
				response.setComment(concept.getComment());

				final ConceptDatatype conceptDatatype = Context.getConceptService().getConceptDatatypeByUuid(concept.getDatatype());
				if (conceptDatatype == null) {
					throw new NullPointerException("Datatype expected");
				}
				response.setDatatype(conceptDatatype);

				if (conceptDatatype.getUuid() == ConceptDatatype.NUMERIC_UUID) {
					final NumericDto numericDetails = concept.getNumericDetails();

					ProposedConceptResponseNumeric proposedConceptResponseNumeric = new ProposedConceptResponseNumeric();
					proposedConceptResponseNumeric.setUnits(numericDetails.getUnits());
					proposedConceptResponseNumeric.setPrecise(numericDetails.getPrecise());
					proposedConceptResponseNumeric.setHiNormal(numericDetails.getHiNormal());
					proposedConceptResponseNumeric.setHiCritical(numericDetails.getHiCritical());
					proposedConceptResponseNumeric.setHiAbsolute(numericDetails.getHiAbsolute());
					proposedConceptResponseNumeric.setLowNormal(numericDetails.getLowNormal());
					proposedConceptResponseNumeric.setLowCritical(numericDetails.getLowCritical());
					proposedConceptResponseNumeric.setLowAbsolute(numericDetails.getLowAbsolute());
					response.setNumericDetails(proposedConceptResponseNumeric);
				}

				final ConceptClass conceptClass = Context.getConceptService().getConceptClassByUuid(concept.getConceptClass());
				if (conceptClass == null) {
					throw new NullPointerException("Concept class expected");
				}
				response.setConceptClass(conceptClass);

				proposedConceptResponsePackage.addProposedConcept(response);
			}
		}

		service.saveProposedConceptResponsePackage(proposedConceptResponsePackage);

		SubmissionResponseDto responseDto = new SubmissionResponseDto();
        responseDto.setId(proposedConceptResponsePackage.getId());
        return responseDto;
    }

	@RequestMapping(value = "/cpm/dictionarymanager/proposalstatus/{proposalId}", method = RequestMethod.GET)
	public @ResponseBody SubmissionStatusDto getSubmissionStatus(@PathVariable int proposalId) {

		final ProposedConceptService service = Context.getService(ProposedConceptService.class);
		final ProposedConceptResponsePackage aPackage = service.getProposedConceptResponsePackageById(proposalId);

		return new SubmissionStatusDto(aPackage.getStatus());
	}

	@ExceptionHandler(APIAuthenticationException.class)
	public void apiAuthenticationExceptionHandler(Exception e, HttpServletResponse response) {

		if (Context.isAuthenticated()) {
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
		} else {
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			response.addHeader("WWW-Authenticate", "Basic realm=\"OpenMRS\"");
		}
	}
}
