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

package unit.uk.gov.hmrc.testuser.controllers

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import common.LogSuppressing
import org.joda.time.LocalDate
import org.mockito.ArgumentMatchers.{any, refEq}
import org.mockito.BDDMockito.given
import org.scalatest.mockito.MockitoSugar
import play.api.Logger
import play.api.http.Status.{CREATED, INTERNAL_SERVER_ERROR, NOT_FOUND, OK}
import play.api.libs.json.Json.toJson
import play.api.libs.json.{JsValue, Json}
import play.api.test._
import uk.gov.hmrc.domain._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.testuser.controllers.TestUserController
import uk.gov.hmrc.testuser.models.JsonFormatters._
import uk.gov.hmrc.testuser.models.UserType.{INDIVIDUAL, ORGANISATION}
import uk.gov.hmrc.testuser.models._
import uk.gov.hmrc.testuser.services.TestUserService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future.failed

class TestUserControllerSpec extends UnitSpec with MockitoSugar with LogSuppressing {

  val user = "user"
  val password = "password"
  val userFullName = "John Doe"
  val emailAddress = "john.doe@example.com"
  val saUtr = "1555369052"
  val nino = "CC333333C"
  val shortNino = "CC333333"
  val mtdItId = "XGIT00000000054"
  val ctUtr = "1555369053"
  val vrn = "999902541"
  val vatRegistrationDate = LocalDate.parse("2011-07-07")
  private val taxOfficeNum = "555"
  private val taxOfficeRef = "EIA000"
  val empRef = s"$taxOfficeNum/$taxOfficeRef"
  val arn = "NARN0396245"
  val lisaManagerReferenceNumber = "Z123456"
  val secureElectronicTransferReferenceNumber = "123456789012"
  val pensionSchemeAdministratorIdentifier = "A1234567"
  val eoriNumber = "GB1234567890"

  val individualDetails = IndividualDetails("John", "Doe", LocalDate.parse("1980-01-10"), Address("221b Baker St", "Marylebone", "NW1 6XE"))
  val organisationDetails = OrganisationDetails("Company ABCDEF", Address("225 Baker St", "Marylebone", "NW1 6XE"))

  val testIndividual = TestIndividual(user, password, userFullName, emailAddress, individualDetails,
    Some(saUtr), Some(nino), Some(mtdItId), Some(vrn), Some(vatRegistrationDate), Some(eoriNumber))

  val testOrganisation = TestOrganisation(user, password, userFullName, emailAddress, organisationDetails,
    Some(saUtr), Some(nino), Some(mtdItId),
    Some(empRef), Some(ctUtr), Some(vrn), Some(vatRegistrationDate), Some(lisaManagerReferenceNumber), Some(secureElectronicTransferReferenceNumber),
    Some(pensionSchemeAdministratorIdentifier), Some(eoriNumber))

  val testAgent = TestAgent(user, password, userFullName, emailAddress, Some(arn))

  val createIndividualServices = Seq(ServiceKeys.NATIONAL_INSURANCE)
  val createOrganisationServices = Seq(ServiceKeys.NATIONAL_INSURANCE)
  val createAgentServices = Seq(ServiceKeys.AGENT_SERVICES)

  trait Setup {
    implicit val actorSystem: ActorSystem = ActorSystem("test")
    implicit val materializer: Materializer = ActorMaterializer()
    implicit val hc = HeaderCarrier()

    val request = FakeRequest()

    def createIndividualRequest = {
      val jsonPayload: JsValue = Json.parse(s"""{"serviceNames":["national-insurance"]}""")
      FakeRequest().withBody[JsValue](jsonPayload)
    }

    def createOrganisationRequest = {
      val jsonPayload: JsValue = Json.parse(s"""{"serviceNames":["national-insurance"]}""")
      FakeRequest().withBody[JsValue](jsonPayload)
    }

    def createAgentRequest = {
      val jsonPayload: JsValue = Json.parse(s"""{"serviceNames":["agent-services"]}""")
      FakeRequest().withBody[JsValue](jsonPayload)
    }

    val underTest = new TestUserController(mock[TestUserService])
  }

  "createIndividual" should {

    "return 201 (Created) with the created individual" in new Setup {

      given(underTest.testUserService.createTestIndividual(refEq(createIndividualServices))(any())).willReturn(testIndividual)

      val result = await(underTest.createIndividual()(createIndividualRequest))

      status(result) shouldBe CREATED
      jsonBodyOf(result) shouldBe toJson(TestIndividualCreatedResponse(user, password, userFullName, emailAddress,
        individualDetails, Some(saUtr), Some(nino), Some(mtdItId), Some(vrn), Some(vatRegistrationDate), Some(eoriNumber)))
    }

    "fail with 500 (Internal Server Error) when the creation of the individual failed" in new Setup {
      withSuppressedLoggingFrom(Logger, "expected test error") { _ =>
        given(underTest.testUserService.createTestIndividual(any())(any()))
          .willReturn(failed(new RuntimeException("expected test error")))

        val result = await(underTest.createIndividual()(createIndividualRequest))

        status(result) shouldBe INTERNAL_SERVER_ERROR
        jsonBodyOf(result) shouldBe toJson(ErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR, "An unexpected error occurred"))
      }
    }

  }

  "createOrganisation" should {

    "return 201 (Created) with the created organisation" in new Setup {

      given(underTest.testUserService.createTestOrganisation(refEq(createOrganisationServices))(any())).willReturn(testOrganisation)

      val result = await(underTest.createOrganisation()(createOrganisationRequest))

      status(result) shouldBe CREATED
      jsonBodyOf(result) shouldBe toJson(TestOrganisationCreatedResponse(user, password, userFullName, emailAddress,
        organisationDetails, Some(saUtr),
        Some(nino), Some(mtdItId), Some(empRef), Some(ctUtr), Some(vrn), Some(vatRegistrationDate), Some(lisaManagerReferenceNumber),
        Some(secureElectronicTransferReferenceNumber), Some(pensionSchemeAdministratorIdentifier), Some(eoriNumber)))
    }

    "fail with 500 (Internal Server Error) when the creation of the organisation failed" in new Setup {
      withSuppressedLoggingFrom(Logger, "expected test error") { _ =>
        given(underTest.testUserService.createTestOrganisation(any())(any()))
          .willReturn(failed(new RuntimeException("expected test error")))

        val result = await(underTest.createOrganisation()(createOrganisationRequest))

        status(result) shouldBe INTERNAL_SERVER_ERROR
        jsonBodyOf(result) shouldBe toJson(ErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR, "An unexpected error occurred"))
      }
    }
  }

  "createAgent" should {

    "return 201 (Created) with the created agent" in new Setup {

      given(underTest.testUserService.createTestAgent(createAgentServices)).willReturn(testAgent)

      val result = await(underTest.createAgent()(createAgentRequest))

      status(result) shouldBe CREATED
      jsonBodyOf(result) shouldBe toJson(TestAgentCreatedResponse(user, password, userFullName, emailAddress, Some(arn)))
    }

    "fail with 500 (Internal Server Error) when the creation of the agent failed" in new Setup {
      withSuppressedLoggingFrom(Logger, "expected test error") { _ =>
        given(underTest.testUserService.createTestAgent(any()))
          .willReturn(failed(new RuntimeException("expected test error")))

        val result = await(underTest.createAgent()(createAgentRequest))

        status(result) shouldBe INTERNAL_SERVER_ERROR
        jsonBodyOf(result) shouldBe toJson(ErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR, "An unexpected error occurred"))
      }
    }
  }

  "fetchIndividualByNino" should {
    "return 200 (Ok) with the individual" in new Setup {

      given(underTest.testUserService.fetchIndividualByNino(refEq(Nino(nino)))(any())).willReturn(testIndividual)

      val result = await(underTest.fetchIndividualByNino(Nino(nino))(request))

      status(result) shouldBe OK
      jsonBodyOf(result) shouldBe Json.toJson(FetchTestIndividualResponse.from(testIndividual))
    }

    "return a 404 (Not Found) when there is no individual matching the NINO" in new Setup {

      given(underTest.testUserService.fetchIndividualByNino(refEq(Nino(nino)))(any())).willReturn(failed(UserNotFound(INDIVIDUAL)))

      val result = await(underTest.fetchIndividualByNino(Nino(nino))(request))

      status(result) shouldBe NOT_FOUND
      jsonBodyOf(result) shouldBe Json.obj("code" -> "USER_NOT_FOUND", "message" -> "The individual can not be found")
    }

    "fail with 500 (Internal Server Error) when fetching the user failed" in new Setup {
      withSuppressedLoggingFrom(Logger, "expected test error") { _ =>
        given(underTest.testUserService.fetchIndividualByNino(refEq(Nino(nino)))(any()))
          .willReturn(failed(new RuntimeException("expected test error")))

        val result = await(underTest.fetchIndividualByNino(Nino(nino))(request))

        status(result) shouldBe INTERNAL_SERVER_ERROR
        jsonBodyOf(result) shouldBe toJson(ErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR, "An unexpected error occurred"))
      }
    }
  }

  "fetchIndividualByShortNino" should {
    "return 200 (Ok) with the individual" in new Setup {

      given(underTest.testUserService.fetchIndividualByShortNino(refEq(NinoNoSuffix(shortNino)))(any())).willReturn(testIndividual)

      val result = await(underTest.fetchIndividualByShortNino(NinoNoSuffix(shortNino))(request))

      status(result) shouldBe OK
      jsonBodyOf(result) shouldBe Json.toJson(FetchTestIndividualResponse.from(testIndividual))
    }

    "return a 404 (Not Found) when there is no individual matching the short nino" in new Setup {

      given(underTest.testUserService.fetchIndividualByShortNino(refEq(NinoNoSuffix(shortNino)))(any())).willReturn(failed(UserNotFound(INDIVIDUAL)))

      val result = await(underTest.fetchIndividualByShortNino(NinoNoSuffix(shortNino))(request))

      status(result) shouldBe NOT_FOUND
      jsonBodyOf(result) shouldBe Json.obj("code" -> "USER_NOT_FOUND", "message" -> "The individual can not be found")
    }

    "fail with 500 (Internal Server Error) when fetching the user failed" in new Setup {
      withSuppressedLoggingFrom(Logger, "expected test error") { _ =>
        given(underTest.testUserService.fetchIndividualByShortNino(refEq(NinoNoSuffix(shortNino)))(any()))
          .willReturn(failed(new RuntimeException("expected test error")))

        val result = await(underTest.fetchIndividualByShortNino(NinoNoSuffix(shortNino))(request))

        status(result) shouldBe INTERNAL_SERVER_ERROR
        jsonBodyOf(result) shouldBe toJson(ErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR, "An unexpected error occurred"))
      }
    }
  }

  "fetchIndividualBySaUtr" should {
    "return 200 (Ok) with the individual" in new Setup {

      given(underTest.testUserService.fetchIndividualBySaUtr(refEq(SaUtr(saUtr)))(any())).willReturn(testIndividual)

      val result = await(underTest.fetchIndividualBySaUtr(SaUtr(saUtr))(request))

      status(result) shouldBe OK
      jsonBodyOf(result) shouldBe Json.toJson(FetchTestIndividualResponse.from(testIndividual))
    }

    "return a 404 (Not Found) when there is no individual matching the saUtr" in new Setup {

      given(underTest.testUserService.fetchIndividualBySaUtr(refEq(SaUtr(saUtr)))(any())).willReturn(failed(UserNotFound(INDIVIDUAL)))

      val result = await(underTest.fetchIndividualBySaUtr(SaUtr(saUtr))(request))

      status(result) shouldBe NOT_FOUND
      jsonBodyOf(result) shouldBe Json.obj("code" -> "USER_NOT_FOUND", "message" -> "The individual can not be found")
    }

    "fail with 500 (Internal Server Error) when fetching the user failed" in new Setup {
      withSuppressedLoggingFrom(Logger, "expected test error") { _ =>
        given(underTest.testUserService.fetchIndividualBySaUtr(refEq(SaUtr(saUtr)))(any()))
          .willReturn(failed(new RuntimeException("expected test error")))

        val result = await(underTest.fetchIndividualBySaUtr(SaUtr(saUtr))(request))

        status(result) shouldBe INTERNAL_SERVER_ERROR
        jsonBodyOf(result) shouldBe toJson(ErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR, "An unexpected error occurred"))
      }
    }
  }

  "fetchOrganisationByEmpref" should {
    "return 200 (Ok) with the organisation" in new Setup {

      given(underTest.testUserService.fetchOrganisationByEmpRef(refEq(EmpRef.fromIdentifiers(empRef)))(any())).willReturn(testOrganisation)

      val result = await(underTest.fetchOrganisationByEmpRef(EmpRef.fromIdentifiers(empRef))(request))

      status(result) shouldBe OK
      jsonBodyOf(result) shouldBe Json.toJson(FetchTestOrganisationResponse.from(testOrganisation))
    }

    "return a 404 (Not Found) when there is no organisation matching the empRef" in new Setup {

      given(underTest.testUserService.fetchOrganisationByEmpRef(refEq(EmpRef.fromIdentifiers(empRef)))(any())).
        willReturn(failed(UserNotFound(ORGANISATION)))

      val result = await(underTest.fetchOrganisationByEmpRef(EmpRef.fromIdentifiers(empRef))(request))

      status(result) shouldBe NOT_FOUND
      jsonBodyOf(result) shouldBe Json.obj("code" -> "USER_NOT_FOUND", "message" -> "The organisation can not be found")
    }

    "fail with 500 (Internal Server Error) when fetching the user failed" in new Setup {
      withSuppressedLoggingFrom(Logger, "expected test error") { _ =>
        given(underTest.testUserService.fetchOrganisationByEmpRef(refEq(EmpRef.fromIdentifiers(empRef)))(any()))
          .willReturn(failed(new RuntimeException("expected test error")))

        val result = await(underTest.fetchOrganisationByEmpRef(EmpRef.fromIdentifiers(empRef))(request))

        status(result) shouldBe INTERNAL_SERVER_ERROR
        jsonBodyOf(result) shouldBe toJson(ErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR, "An unexpected error occurred"))
      }
    }
  }

  "getServices" should {
    "return the services" in new Setup {
      val result = await(underTest.getServices()(request))

      jsonBodyOf(result) shouldBe Json.toJson(Services)
    }
  }
}
