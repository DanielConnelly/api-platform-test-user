/*
 * Copyright 2019 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package unit.uk.gov.hmrc.testuser.connectors

import org.joda.time.LocalDate
import uk.gov.hmrc.domain._
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.testuser.connectors.{Enrolment, GovernmentGatewayLogin, Identifier}
import uk.gov.hmrc.testuser.models._
import uk.gov.hmrc.testuser.models.ServiceKeys._

class GovernmentGatewayLoginSpec extends UnitSpec {
  val user = "user"
  val password = "password"
  val userFullName = "John Doe"
  val emailAddress = "john.doe@example.com"
  val individualDetails = IndividualDetails("John", "Doe", LocalDate.parse("1980-01-10"), Address("221b Baker St", "Marylebone", "NW1 6XE"))
  val organisationDetails = OrganisationDetails("Company ABCDEF",  Address("225 Baker St", "Marylebone", "NW1 6XE"))

  val arn = "NARN0396245"
  val saUtr = "1555369052"
  val nino = "CC333333C"
  val mtdItId = "XGIT00000000054"
  val ctUtr = "1555369053"
  val vrn = "999902541"
  val lisaManRefNum = "Z123456"
  val secureElectronicTransferReferenceNumber = "123456789012"
  val pensionSchemeAdministratorIdentifier = "A1234567"
  val eoriNumber = "GB1234567890"
  private val taxOfficeNumber = "555"
  private val taxOfficeReference = "EIA000"
  val empRef = s"$taxOfficeNumber/$taxOfficeReference"

  val agentEnrolment = Enrolment("HMRC-AS-AGENT", Seq(Identifier("AgentReferenceNumber", arn)))
  val saEnrolment = Enrolment("IR-SA", Seq(Identifier("UTR", saUtr.toString)))
  val mtdItEnrolment = Enrolment("HMRC-MTD-IT", Seq(Identifier("MTDITID", mtdItId.toString)))
  val mtdVatEnrolment = Enrolment("HMRC-MTD-VAT", Seq(Identifier("VRN", vrn.toString)))
  val lisaEnrolment = Enrolment("HMRC-LISA-ORG", Seq(Identifier("ZREF", lisaManRefNum.toString)))
  val setEnrolment = Enrolment("HMRC-SET-ORG", Seq(Identifier("SRN", secureElectronicTransferReferenceNumber.toString)))
  val psaEnrolment = Enrolment("HMRC-PSA-ORG", Seq(Identifier("PSAID", pensionSchemeAdministratorIdentifier.toString))) // Used for Relief at Source
  val ctEnrolment = Enrolment("IR-CT", Seq(Identifier("UTR", ctUtr.toString)))
  val vatEnrolment = Enrolment("HMCE-VATDEC-ORG", Seq(Identifier("VATRegNo", vrn.toString)))
  val payeEnrolment = Enrolment("IR-PAYE", Seq(Identifier("TaxOfficeNumber", taxOfficeNumber),
    Identifier("TaxOfficeReference", taxOfficeReference)))
  val customsEnrolment = Enrolment("HMRC-CUS-ORG", Seq(Identifier("EORINumber", eoriNumber)))

  "A GovernmentGatewayLogin created from a TestAgent" should {

    val agent = TestAgent(user, password, userFullName, emailAddress,
      arn = Some(arn), services = Seq(AGENT_SERVICES))

    "contain no enrolments when the agent has no services" in {
      val login = GovernmentGatewayLogin(agent.copy(services = Seq.empty))

      login.enrolments shouldBe empty
    }

    "contain the enrolment HMRC-AS-AGENT when the agent has the 'agent-services' service" in {
      val login = GovernmentGatewayLogin(agent)

      login.enrolments should contain theSameElementsAs Seq(agentEnrolment)
    }

    "ignore services that are not applicable" in {
      val login = GovernmentGatewayLogin(agent.copy(services = Seq(SELF_ASSESSMENT, AGENT_SERVICES)))

      login.enrolments should contain theSameElementsAs Seq(agentEnrolment)
    }

    "have the credential role populated" in {
      val login = GovernmentGatewayLogin(agent)

      login.credentialRole shouldBe defined
      login.credentialRole shouldBe Some("user")
    }
  }

  "A GovernmentGatewayLogin created from a TestIndividual" should {

    val individual = TestIndividual(user, password, userFullName, emailAddress,individualDetails, Some(saUtr), Some(nino), Some(mtdItId), eoriNumber = Some(eoriNumber), vrn = Some(vrn),
      services = Seq(NATIONAL_INSURANCE, SELF_ASSESSMENT, MTD_INCOME_TAX, CUSTOMS_SERVICES, MTD_VAT))

    "contain no enrolments when the individual has no services" in {
      val login = GovernmentGatewayLogin(individual.copy(services = Seq.empty))

      login.enrolments shouldBe empty
    }

    "contain the right enrolments for the individual's services" in {
      val login = GovernmentGatewayLogin(individual)

      login.enrolments should contain theSameElementsAs Seq(saEnrolment, mtdItEnrolment, customsEnrolment, mtdVatEnrolment)
    }

    "contain the correct enrolments for customs services" in {
      val login = GovernmentGatewayLogin(individual.copy(services = Seq(CUSTOMS_SERVICES)))

      login.enrolments should contain theSameElementsAs Seq(customsEnrolment)
    }

    "contain the correct enrolments for mtd vat" in {
      val login = GovernmentGatewayLogin(individual.copy(services = Seq(MTD_VAT)))

      login.enrolments should contain theSameElementsAs Seq(mtdVatEnrolment)
    }

    "ignore services that are not applicable" in {
      val login = GovernmentGatewayLogin(individual.copy(services = Seq(
        AGENT_SERVICES, NATIONAL_INSURANCE, SELF_ASSESSMENT, MTD_INCOME_TAX, MTD_VAT)))

      login.enrolments should contain theSameElementsAs Seq(saEnrolment, mtdItEnrolment, mtdVatEnrolment)
    }

    "not have the credential role populated" in {
      val login = GovernmentGatewayLogin(individual)

      login.credentialRole shouldBe empty
    }

  }

  "A GovernmentGatewayLogin created from a TestOrganisation" should {

    val organisation = TestOrganisation(user, password, userFullName, emailAddress, organisationDetails,
      Some(saUtr), Some(nino), Some(mtdItId), Some(empRef), Some(ctUtr),
      Some(vrn), lisaManRefNum = Some(lisaManRefNum), secureElectronicTransferReferenceNumber = Some(secureElectronicTransferReferenceNumber),
      pensionSchemeAdministratorIdentifier = Some(pensionSchemeAdministratorIdentifier), eoriNumber = Some(eoriNumber),
      services = Seq(AGENT_SERVICES, NATIONAL_INSURANCE, SELF_ASSESSMENT, CORPORATION_TAX, SUBMIT_VAT_RETURNS,
        PAYE_FOR_EMPLOYERS, MTD_INCOME_TAX, MTD_VAT, LISA, SECURE_ELECTRONIC_TRANSFER, RELIEF_AT_SOURCE, CUSTOMS_SERVICES))

    "contain no enrolments when the organisation has no services" in {
      val login = GovernmentGatewayLogin(organisation.copy(services = Seq.empty))

      login.enrolments shouldBe empty
    }

    "contain the right enrolments for the organisation's services" in {
      val login = GovernmentGatewayLogin(organisation)

      login.enrolments should contain theSameElementsAs
        Seq(saEnrolment, ctEnrolment, vatEnrolment, payeEnrolment, mtdItEnrolment, mtdVatEnrolment, lisaEnrolment, setEnrolment,
          psaEnrolment, customsEnrolment)
    }

    "ignore services that are not applicable" in {
      val login = GovernmentGatewayLogin(organisation.copy(
        services= Seq(AGENT_SERVICES, NATIONAL_INSURANCE, CORPORATION_TAX, SUBMIT_VAT_RETURNS, MTD_VAT, LISA)))

      login.enrolments should contain theSameElementsAs Seq(ctEnrolment, vatEnrolment, mtdVatEnrolment, lisaEnrolment)
    }

    "contain the correct enrolments for the relief at source service" in {
      val login = GovernmentGatewayLogin(organisation.copy(services = Seq(RELIEF_AT_SOURCE)))

      login.enrolments should contain theSameElementsAs Seq(psaEnrolment)
    }

    "contain the correct enrolments for customs services" in {
      val login = GovernmentGatewayLogin(organisation.copy(services = Seq(CUSTOMS_SERVICES)))

      login.enrolments should contain theSameElementsAs Seq(customsEnrolment)
    }

    "not have the credential role populated" in {
      val login = GovernmentGatewayLogin(organisation)

      login.credentialRole shouldBe empty
    }
  }

}
