import os
from pptx import Presentation
from pptx.util import Inches, Pt
from pptx.dml.color import RGBColor
from pptx.enum.text import PP_ALIGN
from pptx.enum.shapes import MSO_SHAPE

def build_presentation():
    prs = Presentation()
    # Set 16:9 widescreen dimensions
    prs.slide_width = Inches(13.333)
    prs.slide_height = Inches(7.5)
    blank_layout = prs.slide_layouts[6] # Blank layout

    # Color Palette
    BG_COLOR = RGBColor(248, 250, 252)       # #F8FAFC
    CARD_BG = RGBColor(255, 255, 255)        # #FFFFFF
    BORDER_COLOR = RGBColor(226, 232, 240)   # #E2E8F0
    TEXT_MAIN = RGBColor(15, 23, 42)         # #0F172A Slate Dark
    TEXT_MUTED = RGBColor(71, 85, 105)       # #475569 Slate Muted
    ACCENT_GREEN = RGBColor(22, 163, 74)     # #16A34A Emerald Green
    ACCENT_TEAL = RGBColor(15, 118, 110)     # #0F766E Deep Teal
    ACCENT_DARK = RGBColor(30, 41, 59)       # #1E293B Dark Slate

    def add_header(slide, title_text, category_text="MEALSHARE CAPSTONE PHASE-1"):
        # Header banner shape
        header_box = slide.shapes.add_textbox(Inches(0.8), Inches(0.4), Inches(11.7), Inches(0.9))
        tf = header_box.text_frame
        tf.word_wrap = True
        tf.margin_left = tf.margin_top = tf.margin_right = tf.margin_bottom = 0

        p0 = tf.paragraphs[0]
        p0.text = category_text.upper()
        p0.font.size = Pt(10)
        p0.font.bold = True
        p0.font.color.rgb = ACCENT_GREEN

        p1 = tf.add_paragraph()
        p1.text = title_text
        p1.font.size = Pt(22)
        p1.font.bold = True
        p1.font.color.rgb = TEXT_MAIN

    def add_notes(slide, notes_text):
        notes_slide = slide.notes_slide
        tf = notes_slide.notes_text_frame
        tf.text = notes_text

    def create_card(slide, left, top, width, height, title, items, card_bg=CARD_BG, border_color=BORDER_COLOR):
        # Card shape
        shape = slide.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, left, top, width, height)
        shape.fill.solid()
        shape.fill.fore_color.rgb = card_bg
        shape.line.color.rgb = border_color
        shape.line.width = Pt(1.5)

        tb = slide.shapes.add_textbox(left + Inches(0.2), top + Inches(0.2), width - Inches(0.4), height - Inches(0.4))
        tf = tb.text_frame
        tf.word_wrap = True
        tf.margin_left = tf.margin_top = tf.margin_right = tf.margin_bottom = 0

        if title:
            p_title = tf.paragraphs[0]
            p_title.text = title
            p_title.font.size = Pt(15)
            p_title.font.bold = True
            p_title.font.color.rgb = ACCENT_TEAL
            p_title.space_after = Pt(8)
            first_item = True
        else:
            first_item = False

        for idx, item in enumerate(items):
            if first_item and idx == 0 and not title:
                p = tf.paragraphs[0]
            else:
                p = tf.add_paragraph()
            p.text = "• " + item
            p.font.size = Pt(12)
            p.font.color.rgb = TEXT_MAIN
            p.space_after = Pt(5)

    # ==================== SLIDE 1: TITLE ====================
    slide1 = prs.slides.add_slide(blank_layout)
    # Background
    bg = slide1.shapes.add_shape(MSO_SHAPE.RECTANGLE, 0, 0, Inches(13.333), Inches(7.5))
    bg.fill.solid()
    bg.fill.fore_color.rgb = RGBColor(241, 245, 249)
    bg.line.fill.background()

    # Title Card
    tcard = slide1.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(1.2), Inches(1.0), Inches(10.933), Inches(5.5))
    tcard.fill.solid()
    tcard.fill.fore_color.rgb = CARD_BG
    tcard.line.color.rgb = ACCENT_GREEN
    tcard.line.width = Pt(2.5)

    tb = slide1.shapes.add_textbox(Inches(1.6), Inches(1.4), Inches(10.133), Inches(4.7))
    tf = tb.text_frame
    tf.word_wrap = True

    p = tf.paragraphs[0]
    p.text = "UQ25CA741A — CAPSTONE PROJECT PHASE-1 (PRESENTATION-2)"
    p.font.size = Pt(12)
    p.font.bold = True
    p.font.color.rgb = ACCENT_GREEN

    p2 = tf.add_paragraph()
    p2.text = "MealShare"
    p2.font.size = Pt(36)
    p2.font.bold = True
    p2.font.color.rgb = ACCENT_DARK
    p2.space_before = Pt(8)

    p3 = tf.add_paragraph()
    p3.text = "A Geographically Distributed Surplus Food Redistribution & Coordination Platform"
    p3.font.size = Pt(18)
    p3.font.bold = True
    p3.font.color.rgb = ACCENT_TEAL
    p3.space_after = Pt(24)

    p4 = tf.add_paragraph()
    p4.text = "Candidate: Akshar Kakadiya | USN: PES1PG24CA014"
    p4.font.size = Pt(14)
    p4.font.bold = True
    p4.font.color.rgb = TEXT_MAIN

    p5 = tf.add_paragraph()
    p5.text = "Department of Computer Applications | PES University, Bengaluru"
    p5.font.size = Pt(13)
    p5.font.color.rgb = TEXT_MUTED

    p6 = tf.add_paragraph()
    p6.text = "Academic Year: 2026–2027 | Master of Computer Applications (MCA Sem 3)"
    p6.font.size = Pt(13)
    p6.font.color.rgb = TEXT_MUTED
    p6.space_before = Pt(4)

    add_notes(slide1, "Good morning respected evaluators, committee members, and faculty guide. My name is Akshar Kakadiya, USN PES1PG24CA014. Today, I am presenting MealShare, a geographically distributed surplus food redistribution platform engineered for our Phase-1 Second Presentation.")

    # ==================== SLIDE 2: ABSTRACT ====================
    slide2 = prs.slides.add_slide(blank_layout)
    add_header(slide2, "Abstract & Executive Summary")
    create_card(slide2, Inches(0.8), Inches(1.5), Inches(5.6), Inches(2.6), "The Problem Scenario", [
        "Millions of tons of edible food discarded daily in urban dining centers.",
        "Strict 3–6 hour consumption window for cooked meals.",
        "Severe information asymmetry and missing volunteer transport."
    ])
    create_card(slide2, Inches(6.8), Inches(1.5), Inches(5.6), Inches(2.6), "The MealShare Solution", [
        "Unified 4-role platform: Donor, Receiver, Volunteer, and Admin.",
        "Automated proximity discovery via PostGIS spatial engine.",
        "Guaranteed custody transfers via 6-digit cryptographic OTP."
    ])
    create_card(slide2, Inches(0.8), Inches(4.4), Inches(5.6), Inches(2.5), "Approved Technology Stack", [
        "Flutter (Cross-platform field mobile apps for 3 roles).",
        "React 18 + Vite + TypeScript (Administrative Web Portal).",
        "Spring Boot 3.4.x (REST API) + PostgreSQL 16 & PostGIS 3.4."
    ])
    create_card(slide2, Inches(6.8), Inches(4.4), Inches(5.6), Inches(2.5), "Verified Implementation Metrics", [
        "47 REST API endpoints across 18 Spring Boot controllers.",
        "10 relational and spatial database tables via 7 Flyway migrations.",
        "204 backend tests passing with zero failures; clean UI builds."
    ])
    add_notes(slide2, "Our abstract encapsulates an end-to-end software engineering solution to urban food waste. The platform coordinates four user roles in a single Spring Boot and PostGIS backend, enforcing spatial discovery, concurrency-safe claim allocations, and cryptographic OTP handovers, backed by 204 passing automated tests.")

    # ==================== SLIDE 3: PROBLEM STATEMENT ====================
    slide3 = prs.slides.add_slide(blank_layout)
    add_header(slide3, "Problem Statement: The Redistribution Breakdown")
    create_card(slide3, Inches(0.8), Inches(1.6), Inches(3.6), Inches(5.2), "1. Generation & Perishability", [
        "Urban commercial kitchens routinely produce 50–200+ surplus portions.",
        "Prepared food has a tight 3 to 6-hour consumption window.",
        "Delays in connecting with recipients cause rapid microbial spoilage.",
        "Manual phone outreach wastes critical hours."
    ])
    create_card(slide3, Inches(4.8), Inches(1.6), Inches(3.6), Inches(5.2), "2. Coordination & Logistics Void", [
        "Donors lack visibility into nearby shelters with current demand.",
        "Recipient shelters operate in silos and lack dedicated transport vehicles.",
        "Ad-hoc WhatsApp groups cause chaotic, duplicate travel for same donation.",
        "Secondary waste occurs if food is over-claimed."
    ])
    create_card(slide3, Inches(8.8), Inches(1.6), Inches(3.6), Inches(5.2), "3. Custody & Liability Risks", [
        "Informal handovers lack physical proof-of-custody (phantom pickups).",
        "Commercial food donors fear legal liability for foodborne illnesses.",
        "Absence of mandatory food safety declarations causes food dumping.",
        "Zero administrative audit trail for compliance."
    ])
    add_notes(slide3, "This visual problem chain shows why good food ends up in landfills. When a hotel has surplus meals, they face a 3-to-6 hour consumption window. They lack visibility into nearby shelters, recipient shelters lack vehicles, and unverified handovers introduce fraud and liability risks. MealShare addresses each failure point.")

    # ==================== SLIDE 4: EXISTING SYSTEMS ====================
    slide4 = prs.slides.add_slide(blank_layout)
    add_header(slide4, "Existing Systems & Operational Deficiencies")
    create_card(slide4, Inches(0.8), Inches(1.6), Inches(5.6), Inches(2.5), "Manual & Social Channels (WhatsApp / Phone)", [
        "Broadcasts reach individuals too far away; no spatial radius filtering.",
        "No concurrency control on claims, causing race conditions.",
        "Completely unverified handovers with zero regulatory audit trail."
    ])
    create_card(slide4, Inches(6.8), Inches(1.6), Inches(5.6), Inches(2.5), "Academic Prototypes (2021–2024)", [
        "Connect only 2 parties (Donor ↔ Recipient); omit volunteer logistics.",
        "Client-side GPS math causes severe mobile lag and battery drain.",
        "Rely on unverified 'click-to-confirm' buttons for pickup verification."
    ])
    create_card(slide4, Inches(0.8), Inches(4.4), Inches(5.6), Inches(2.5), "Commercial Platforms (Too Good To Go, OLIO)", [
        "Too Good To Go operates a commercial B2C discounted retail model.",
        "Customer self-pickup; not geared for charity food distribution.",
        "OLIO focuses on informal peer-to-peer household item sharing."
    ])
    create_card(slide4, Inches(6.8), Inches(4.4), Inches(5.6), Inches(2.5), "Enterprise Food Banks (MealConnect)", [
        "Built for scheduled warehouse pallets and fleet semi-trucks.",
        "Inflexible for hyper-local urban rescue within a 2-hour window.",
        "High bureaucratic overhead for grassroots shelters."
    ])
    add_notes(slide4, "Existing channels fall short in urban scenarios. WhatsApp groups lack spatial awareness and cause race conditions. Academic prototypes omit volunteer logistics and rely on unverified clicks. Commercial apps like Too Good To Go focus on consumer sales. MealShare provides a lightweight, secure system with integrated volunteer dispatch.")

    # ==================== SLIDE 5: PROPOSED SOLUTION ====================
    slide5 = prs.slides.add_slide(blank_layout)
    add_header(slide5, "Proposed Solution: The MealShare Platform")
    create_card(slide5, Inches(0.8), Inches(1.6), Inches(2.7), Inches(5.2), "Donor Role", [
        "Publish surplus listings.",
        "Mandatory food safety self-declaration toggle.",
        "Specify pickup time window.",
        "Review FCFS claims and approve bounded quantity."
    ])
    create_card(slide5, Inches(3.8), Inches(1.6), Inches(2.7), Inches(5.2), "Receiver Role", [
        "Discover unexpired meals via PostGIS radius search.",
        "Inspect preparation time and safety declaration.",
        "Submit quantity claims with organizational justification.",
        "Track delivery state."
    ])
    create_card(slide5, Inches(6.8), Inches(1.6), Inches(2.7), Inches(5.2), "Volunteer Role", [
        "Assigned delivery queue with donor/shelter guidance.",
        "Dynamic availability status (AVAILABLE, BUSY).",
        "Authenticate collection via 6-digit cryptographic OTP.",
        "Confirm final handover."
    ])
    create_card(slide5, Inches(9.8), Inches(1.6), Inches(2.7), Inches(5.2), "Admin Role", [
        "Institutional receiver verification queue.",
        "User account adjudication (ACTIVE, SUSPENDED).",
        "Courier assignment modal.",
        "Immutable PostgreSQL JSONB audit log inspection."
    ])
    add_notes(slide5, "This diagram illustrates our proposed solution. The MealShare platform acts as an intelligent coordinator: donors publish listings with food safety declarations; nearby shelters discover them through PostGIS queries; donors approve bounded claims; administrators assign available volunteers; and volunteers authenticate collections using a cryptographic OTP.")

    # ==================== SLIDE 6: OBJECTIVES ====================
    slide6 = prs.slides.add_slide(blank_layout)
    add_header(slide6, "Measurable Project Objectives")
    create_card(slide6, Inches(0.8), Inches(1.6), Inches(5.6), Inches(2.5), "OBJ 1: Multi-Role RBAC & Security", [
        "Firebase token authentication with database-enforced RBAC.",
        "Role-isolated endpoints (DONOR, RECEIVER, VOLUNTEER, ADMIN).",
        "Instant lockout for suspended accounts (ROLE_SUSPENDED)."
    ])
    create_card(slide6, Inches(6.8), Inches(1.6), Inches(5.6), Inches(2.5), "OBJ 2: Publishing & Safety Declarations", [
        "Standardized listing capture across 8 food categories.",
        "Mandatory donor food safety declaration (foodSafetyDeclared).",
        "Temporal boundaries enforced (prep <= NOW, consume > NOW)."
    ])
    create_card(slide6, Inches(0.8), Inches(4.4), Inches(5.6), Inches(2.5), "OBJ 3: PostGIS Spatial Proximity", [
        "Server-side PostGIS spatial point storage (SRID 4326).",
        "GiST-indexed ST_DWithin and ST_Distance radius filtering.",
        "Automated exclusion of expired donations (consume > NOW)."
    ])
    create_card(slide6, Inches(6.8), Inches(4.4), Inches(5.6), Inches(2.5), "OBJ 4: Concurrency Allocation & OTP", [
        "First-Come, First-Served chronological claim ordering.",
        "Atomic quantity allocation with DB check constraints.",
        "6-digit SecureRandom OTP hashed via BCrypt for pickup proof."
    ])
    add_notes(slide6, "We defined 8 measurable technical objectives for Phase-1. Every objective—from database-authoritative RBAC and PostGIS spatial indexing, to atomic FCFS allocation, BCrypt-hashed OTP handovers, and JSONB audit trails—has been completely implemented and validated with automated tests.")

    # ==================== SLIDE 7: SCOPE ====================
    slide7 = prs.slides.add_slide(blank_layout)
    add_header(slide7, "Project Scope & Architectural Boundaries")
    create_card(slide7, Inches(0.8), Inches(1.6), Inches(5.6), Inches(5.2), "In-Scope Phase-1 Implementation", [
        "Full-Stack Multi-Role Architecture (4 user roles).",
        "47 REST API endpoints across 18 Spring Boot controllers.",
        "10 PostgreSQL tables managed by 7 versioned Flyway migrations.",
        "PostGIS GiST spatial indexing for proximity discovery.",
        "Concurrency-safe FCFS request allocation with check constraints.",
        "Cryptographic 6-digit BCrypt-hashed OTP handover protocol.",
        "Decoupled Spring Application Events for operational alerts.",
        "React 18 Admin Web portal with 10 management/analytics pages.",
        "Flutter cross-platform mobile client with 17 screens.",
        "204 automated unit and integration tests passing."
    ])
    create_card(slide7, Inches(6.8), Inches(1.6), Inches(5.6), Inches(2.5), "Operational & Legal Boundaries", [
        "Food Safety: Digital self-declaration protocol only; platform does NOT certify food or assume legal liability.",
        "Push Alerts: In-app PostgreSQL alerts verified; physical mobile FCM push delivery is NOT TESTED (dev mode)."
    ])
    create_card(slide7, Inches(6.8), Inches(4.4), Inches(5.6), Inches(2.4), "Future Scope (Phase-2 Roadmap)", [
        "Google OR-Tools multi-stop vehicle route optimization (VRPTW).",
        "Predictive machine learning donor supply forecasting.",
        "Automated background expiration daemon via Quartz."
    ])
    add_notes(slide7, "We maintain strict academic transparency regarding our project scope. We clearly delineate our fully implemented Phase-1 deliverables—47 REST endpoints, 10 database tables, PostGIS spatial engine, and Flutter apps—from our operational boundaries. Food safety is self-declared rather than lab-certified, and machine learning route optimization is established as our future roadmap.")

    # ==================== SLIDE 8: LITERATURE SURVEY OVERVIEW ====================
    slide8 = prs.slides.add_slide(blank_layout)
    add_header(slide8, "Literature Survey: 12 Verified Research Papers")
    create_card(slide8, Inches(0.8), Inches(1.6), Inches(3.6), Inches(5.2), "Corpus & Indexing", [
        "Total Papers: 12 Verified.",
        "Date Window: 2021–2025 (100%).",
        "Publisher Distribution:",
        "  • IEEE Xplore: 9 Papers (75%)",
        "  • Elsevier: 2 Papers (17%)",
        "  • Springer: 1 Paper (8%)",
        "Exact DOIs and authors verified.",
        "Zero unverified preprints."
    ])
    create_card(slide8, Inches(4.8), Inches(1.6), Inches(7.6), Inches(5.2), "Summary of Reviewed Research Domains", [
        "Theme 1: Digital Redistribution Platforms (Pratama 2023, Sudheepa 2023, Karani 2024)",
        "  Proves mobile role separation and cross-platform access maximize user retention.",
        "Theme 2: Food Waste Reduction Frameworks (Trevisan & Formentini 2024)",
        "  Establishes that digital platforms fail without integrated logistics and governance.",
        "Theme 3: Spatial Proximity Matching (Patel & Pandey 2023, Varghese 2021)",
        "  Demonstrates geographic proximity reduces discovery time; highlights client-side bottlenecks.",
        "Theme 4: Volunteer Coordination & Vehicle Routing (Pai 2023, Reusken 2024)",
        "  Proves volunteer status tracking cuts turnaround; models food bank VRPTW time windows.",
        "Theme 5: Traceability & Handover Security (Villegas-Ch 2025, Chhibber 2021)",
        "  Proves cryptographic custody transfers prevent fraud; validates cloud synchronization.",
        "Theme 6: Demand Forecasting & Impact (Paul & Davis 2022, Damiani 2021)",
        "  ML donor forecasting; LCA confirms urban radius < 15 km maximizes carbon offset."
    ])
    add_notes(slide8, "Our literature survey comprises 12 verified academic papers published between 2021 and 2025. 75% of our papers come from IEEE publications, with the remainder from high-impact Elsevier and Springer operations research journals. We systematically analyzed previous work across six domains: digital platforms, spatial matching, volunteer logistics, handover security, demand forecasting, and environmental impact.")

    # ==================== SLIDE 9: PAPERS 1-6 ====================
    slide9 = prs.slides.add_slide(blank_layout)
    add_header(slide9, "Literature Survey: Systems & Mobile Platforms (1–6)")
    create_card(slide9, Inches(0.8), Inches(1.6), Inches(5.6), Inches(1.6), "1. Trevisan & Formentini (2024) — IEEE Trans. Eng. Manage.", [
        "Focus: Industry 4.0 food loss framework [DOI: 10.1109/TEM.2023.3273110].",
        "Finding: Digital platforms fail without multi-stakeholder governance."
    ])
    create_card(slide9, Inches(6.8), Inches(1.6), Inches(5.6), Inches(1.6), "2. Patel & Pandey (2023) — IEEE ICACITE", [
        "Focus: Location-based Android app [DOI: 10.1109/ICACITE57410.2023.10183202].",
        "Finding: Client-side math causes lag; motivates server-side PostGIS."
    ])
    create_card(slide9, Inches(0.8), Inches(3.4), Inches(5.6), Inches(1.6), "3. Pratama et al. (2023) — IEEE ICCED", [
        "Focus: Food sharing UI/UX design [DOI: 10.1109/ICCED60214.2023.10425590].",
        "Finding: Distinct role workflows maximize community retention."
    ])
    create_card(slide9, Inches(6.8), Inches(3.4), Inches(5.6), Inches(1.6), "4. Sudheepa et al. (2023) — IEEE ICAAIC", [
        "Focus: Broadcast-and-claim lifecycle [DOI: 10.1109/ICAAIC56838.2023.10140489].",
        "Finding: Formalized by MealShare's atomic allocation check constraints."
    ])
    create_card(slide9, Inches(0.8), Inches(5.2), Inches(5.6), Inches(1.6), "5. Karani et al. (2024) — IEEE AICTC", [
        "Focus: Cross-platform WasteLess app [DOI: 10.1109/AICTC58357.2024.10735008].",
        "Finding: Multi-OS support boosts adoption by > 40%; justifies Flutter."
    ])
    create_card(slide9, Inches(6.8), Inches(5.2), Inches(5.6), Inches(1.6), "6. Pai et al. (2023) — IEEE CSITSS", [
        "Focus: NGO CONNECT volunteer tracking [DOI: 10.1109/CSITSS60515.2023.10334076].",
        "Finding: Volunteer status tracking (AVAILABLE, BUSY) cuts task delay."
    ])
    add_notes(slide9, "Papers 1 to 6 focus on digital platforms and mobile design. Trevisan emphasizes the necessity of multi-party governance. Patel & Pandey explored location-based matching, but their reliance on client-side math motivated our choice of server-side PostGIS. Karani proved that cross-platform mobile access is vital, confirming our choice of Flutter. Finally, Pai et al. motivated our volunteer availability tracking model.")

    # ==================== SLIDE 10: PAPERS 7-12 ====================
    slide10 = prs.slides.add_slide(blank_layout)
    add_header(slide10, "Literature Survey: Logistics, Security & Forecasting (7–12)")
    create_card(slide10, Inches(0.8), Inches(1.6), Inches(5.6), Inches(1.6), "7. Villegas-Ch et al. (2025) — IEEE Access", [
        "Focus: Perishable food traceability [DOI: 10.1109/ACCESS.2025.3561596].",
        "Finding: Cryptographic custody verification eliminates handover disputes."
    ])
    create_card(slide10, Inches(6.8), Inches(1.6), Inches(5.6), Inches(1.6), "8. Varghese et al. (2021) — IEEE CCWC", [
        "Focus: Smart city donation app (SeVa) [DOI: 10.1109/CCWC51732.2021.9375945].",
        "Finding: Real-time event notifications accelerate claim rates."
    ])
    create_card(slide10, Inches(0.8), Inches(3.4), Inches(5.6), Inches(1.6), "9. Chhibber et al. (2021) — IEEE ICCE", [
        "Focus: Cloud supply chain sync [DOI: 10.1109/ICCE50685.2021.9427641].",
        "Finding: Centralized cloud state reduces distribution latency by > 35%."
    ])
    create_card(slide10, Inches(6.8), Inches(3.4), Inches(5.6), Inches(1.6), "10. Reusken et al. (2024) — Eur. J. Oper. Res.", [
        "Focus: Vehicle routing for food banks [DOI: 10.1016/j.ejor.2024.03.031].",
        "Finding: Theoretical VRPTW model; foundation for future OR-Tools."
    ])
    create_card(slide10, Inches(0.8), Inches(5.2), Inches(5.6), Inches(1.6), "11. Paul & Davis (2022) — Annals of Oper. Res.", [
        "Focus: Ensemble ML supply forecasting [DOI: 10.1007/s10479-021-04146-5].",
        "Finding: Donor clustering improves volume prediction by 22–34%."
    ])
    create_card(slide10, Inches(6.8), Inches(5.2), Inches(5.6), Inches(1.6), "12. Damiani et al. (2021) — J. Cleaner Production", [
        "Focus: LCA environmental offsets [DOI: 10.1016/j.jclepro.2021.125813].",
        "Finding: Urban radius < 15 km optimizes net greenhouse gas savings."
    ])
    add_notes(slide10, "Papers 7 through 12 establish our theoretical foundations in security, logistics, and environmental impact. Villegas-Ch demonstrated the critical need for cryptographic handover verification, which we implemented via BCrypt OTP. Damiani proved that localized redistribution under 15 km maximizes carbon offsets, validating our 10 km PostGIS radius. Finally, Reusken and Paul & Davis establish the theoretical models for our future routing and forecasting roadmap.")

    # ==================== SLIDE 11: COMPARATIVE ANALYSIS ====================
    slide11 = prs.slides.add_slide(blank_layout)
    add_header(slide11, "Comparative Analysis: Existing Approaches vs. MealShare")
    create_card(slide11, Inches(0.8), Inches(1.6), Inches(11.6), Inches(5.2), "Comprehensive Capability Comparison Matrix", [
        "Patel & Pandey (2023) [IEEE]: Spatial Discovery ✓ | Concurrency — | Volunteer Logistics — | Secure Handover — | Audit Trail —",
        "Sudheepa et al. (2023) [IEEE]: Spatial Discovery — | Concurrency — | Volunteer Logistics — | Secure Handover — | Audit Trail —",
        "Pai et al. (2023) [IEEE]: Spatial Discovery — | Concurrency — | Volunteer Logistics ✓ | Secure Handover — | Audit Trail —",
        "Villegas-Ch et al. (2025) [IEEE]: Spatial Discovery — | Concurrency — | Volunteer Logistics — | Secure Handover ✓ (Ledger) | Audit Trail ✓",
        "OLIO (Commercial P2P): Spatial Discovery ✓ (Radius) | Concurrency — | Volunteer Logistics — | Secure Handover — | Audit Trail —",
        "Too Good To Go (Commercial B2C): Spatial Discovery ✓ (Map) | Concurrency ✓ (Bags) | Volunteer Logistics — | Secure Handover — | Audit Trail —",
        "MealConnect (Enterprise NGO): Spatial Discovery ✓ (Zip) | Concurrency ✓ | Volunteer Logistics ✓ (Fleet) | Secure Handover — | Audit Trail ✓",
        "MEALSHARE (Implemented): Spatial Discovery ✓ (PostGIS) | Concurrency ✓ (Atomic FCFS) | Volunteer Logistics ✓ (Tri-State) | Secure Handover ✓ (6-Digit OTP) | Audit Trail ✓ (JSONB)",
        "Key Synthesis: MealShare uniquely integrates server-side spatial indexing, atomic FCFS allocation, volunteer dispatch, and cryptographic OTP handovers into a lightweight, non-profit platform."
    ])
    add_notes(slide11, "This comparative matrix evaluates existing systems across five critical engineering dimensions. Academic prototypes generally omit volunteer logistics and handover security. Specialized blockchain studies achieve security but require expensive hardware. Commercial platforms like Too Good To Go focus on consumer sales. MealShare occupies a unique sweet spot: lightweight enough for community adoption, yet engineered with server-side spatial indexing, atomic concurrency control, and cryptographic OTP handovers.")

    # ==================== SLIDE 12: RESEARCH GAP ====================
    slide12 = prs.slides.add_slide(blank_layout)
    add_header(slide12, "Identified Research Gaps & MealShare Integration")
    create_card(slide12, Inches(0.8), Inches(1.6), Inches(5.6), Inches(2.5), "Identified Deficiencies in Literature", [
        "G1: Role Fragmentation (no unified Donor-Receiver-Volunteer-Admin system).",
        "G2: Spatial Scalability (heavy client-side math; no database spatial index).",
        "G3: Handover Vulnerabilities (unverified clicks vs. expensive blockchain)."
    ])
    create_card(slide12, Inches(6.8), Inches(1.6), Inches(5.6), Inches(2.5), "Identified Deficiencies in Literature", [
        "G4: Neglected Food Safety (no legal self-declarations or temporal filters).",
        "G5: OR / ML Disconnect (theoretical models isolated from software apps).",
        "G6: Lack of Auditability (no append-only logs or admin vetting queues)."
    ])
    create_card(slide12, Inches(0.8), Inches(4.4), Inches(11.6), Inches(2.4), "The MealShare Integrated Engineering Bridge", [
        "MealShare unifies: Food Donation Publishing + PostGIS Spatial Proximity + Concurrency-Safe Allocation + Volunteer Dispatch + Cryptographic OTP Handover + Immutable JSONB Audit Logs.",
        "Bridges the gap between operations research theory and deployed field software."
    ])
    add_notes(slide12, "Our synthesis identified six clear research gaps. Existing academic research addresses individual pieces in isolation: theoretical vehicle routing models exist without software, while software prototypes exist without spatial indexing or handover security. MealShare bridges this divide by delivering an integrated, production-aligned platform combining spatial discovery, concurrency-safe allocation, volunteer dispatch, and cryptographic OTP handovers.")

    # ==================== SLIDE 13: MEALSHARE CONTRIBUTION ====================
    slide13 = prs.slides.add_slide(blank_layout)
    add_header(slide13, "MealShare Platform Contributions")
    create_card(slide13, Inches(0.8), Inches(1.6), Inches(5.6), Inches(5.2), "Implemented Contributions (Phase-1)", [
        "1. Standardized Surplus Food Publishing (8 categories).",
        "2. Embedded Food Safety Self-Declarations & Limits.",
        "3. Server-Side PostGIS Spatial Proximity Engine.",
        "4. Multi-Stage Receiver Request Workflow.",
        "5. Concurrency-Safe FCFS Allocation (Check Constraints).",
        "6. Volunteer Availability Tracking (AVAILABLE, BUSY).",
        "7. Cryptographic OTP Handover Protocol (SecureRandom).",
        "8. Decoupled Spring Event Notification Pipeline.",
        "9. Immutable PostgreSQL JSONB Audit Trail.",
        "10. Centralized React 18 Admin Governance Portal."
    ])
    create_card(slide13, Inches(6.8), Inches(1.6), Inches(5.6), Inches(5.2), "Future Algorithmic Roadmap (Phase-2)", [
        "1. Automated Multi-Stop Route Optimization:",
        "   Ingests PostGIS coordinates and pickup time windows to",
        "   solve VRPTW multi-stop routing via Google OR-Tools.",
        "2. Predictive Machine Learning Donor Forecasting:",
        "   Ensemble regression models forecasting surplus volume",
        "   patterns based on historical PostgreSQL frequency data.",
        "3. Intelligent Multi-Criteria Dynamic Claim Matching.",
        "4. Production APNS / FCM Background Mobile Push Alerts.",
        "Note: Future capabilities are clearly delineated."
    ])
    add_notes(slide13, "This slide details our ten concrete engineering contributions. Every feature listed under Implemented is fully functional and backed by unit and integration tests. We also establish a clear future roadmap: Google OR-Tools route optimization and machine learning demand prediction are documented as future research extensions, ensuring our current presentation remains 100% honest and verifiable.")

    # ==================== SLIDE 14: TECH STACK ====================
    slide14 = prs.slides.add_slide(blank_layout)
    add_header(slide14, "Approved Tools & Technologies")
    create_card(slide14, Inches(0.8), Inches(1.6), Inches(3.6), Inches(5.2), "Client Applications", [
        "Admin Web Client:",
        "  • React 18.3 & Vite 8",
        "  • TypeScript 5.8",
        "  • Material-UI Component Suite",
        "  • Recharts Analytics Engine",
        "  • Port: 5173",
        "Mobile Field Applications:",
        "  • Flutter 3.44.0 & Dart 3.7",
        "  • Provider State Management",
        "  • Material 3 Design",
        "  • Role-Separated Shells"
    ])
    create_card(slide14, Inches(4.8), Inches(1.6), Inches(3.6), Inches(5.2), "Backend API & Security", [
        "Application Server:",
        "  • Spring Boot 3.4.3",
        "  • Java 17 LTS",
        "  • Spring Security 6",
        "  • Spring Data JPA / Hibernate",
        "  • BCrypt Password Encoder",
        "  • SpringDoc OpenAPI 2.8",
        "  • Port: 8081",
        "Identity Provider:",
        "  • Firebase Authentication",
        "  • Server Admin SDK Verification"
    ])
    create_card(slide14, Inches(8.8), Inches(1.6), Inches(3.6), Inches(5.2), "Data Layer & Persistence", [
        "Database Engine:",
        "  • PostgreSQL 16.8 (Docker: 5433)",
        "  • PostGIS 3.4.3 Spatial Ext.",
        "Schema Management:",
        "  • Flyway 10.x Migrations (V1–V7)",
        "  • Immutable SQL Versioning",
        "Spatial Indexing:",
        "  • GEOMETRY(Point, 4326)",
        "  • GiST Spatial Index",
        "Audit Trail: JSONB Storage"
    ])
    add_notes(slide14, "We selected modern, production-aligned technologies. Our frontend combines a Flutter mobile client for field users with a React 18 TypeScript web portal for administrators. Our backend is built on Spring Boot 3.4 and Java 17, exposing 47 REST endpoints. For persistence, we run PostgreSQL 16 with the PostGIS 3.4 spatial extension, with all schema changes version-controlled via Flyway.")

    # ==================== SLIDE 15: ARCHITECTURE ====================
    slide15 = prs.slides.add_slide(blank_layout)
    add_header(slide15, "System Architecture & Security Boundary")
    create_card(slide15, Inches(0.8), Inches(1.6), Inches(11.6), Inches(5.2), "Layered Architecture Flow", [
        "1. Client Tier: React 18 Admin Web (Desktop) + Flutter Mobile (Donor, Receiver, Volunteer).",
        "2. Authentication Boundary: Clients sign in via Firebase Auth, receiving signed JWT Bearer tokens.",
        "3. Security Gateway: FirebaseAuthenticationFilter intercepts every API request, decodes the token, and queries PostgreSQL users table directly to resolve granted authorities (ROLE_DONOR, ROLE_ADMIN, etc.).",
        "4. Application Server: Spring Boot 3.4.x backend exposing 47 endpoints across 18 controllers with transactional services.",
        "5. Decoupled Event Bus: Domain events dispatched via ApplicationEventPublisher; handled AFTER_COMMIT for notification persistence.",
        "6. Data Persistence: PostgreSQL 16.8 with PostGIS 3.4.3 storing 10 relational tables, GiST spatial index, and append-only JSONB audit logs."
    ])
    add_notes(slide15, "This diagram represents our implemented system architecture. Clients obtain JWT tokens from Firebase, which our Spring Boot security filter decodes. Crucially, roles are resolved strictly from PostgreSQL, preventing client privilege escalation. The backend processes business transactions, coordinates PostGIS spatial queries, and dispatches decoupled Spring Application Events to persist notifications and record JSONB audit logs.")

    # ==================== SLIDE 16: FUNCTIONAL REQUIREMENTS ====================
    slide16 = prs.slides.add_slide(blank_layout)
    add_header(slide16, "Functional Requirements by Role")
    create_card(slide16, Inches(0.8), Inches(1.6), Inches(2.7), Inches(5.2), "Donor (10 APIs)", [
        "Profile & geocoordinates.",
        "Create surplus donation.",
        "Accept food safety terms.",
        "Set pickup time window.",
        "View active listings.",
        "Review FCFS claims.",
        "Approve bounded allocation.",
        "Reject claim with reason."
    ])
    create_card(slide16, Inches(3.8), Inches(1.6), Inches(2.7), Inches(5.2), "Receiver (8 APIs)", [
        "Organization profile & radius.",
        "PostGIS nearby discovery.",
        "Inspect food & declarations.",
        "Submit bounded claim.",
        "Active claim uniqueness.",
        "View claim history.",
        "Withdraw pending claim."
    ])
    create_card(slide16, Inches(6.8), Inches(1.6), Inches(2.7), Inches(5.2), "Volunteer (4 APIs)", [
        "Assigned delivery list.",
        "IDOR ownership checks.",
        "Dynamic status (AVAILABLE).",
        "Inspect pickup details.",
        "Verify 6-digit OTP.",
        "Confirm delivery completion."
    ])
    create_card(slide16, Inches(9.8), Inches(1.6), Inches(2.7), Inches(5.2), "Admin (16 APIs)", [
        "Live KPI dashboard.",
        "Receiver verification queue.",
        "User status adjudication.",
        "Global donation oversight.",
        "Volunteer dispatch modal.",
        "Aggregated volume reports.",
        "Inspect JSONB audit trail."
    ])
    add_notes(slide16, "We organized our functional requirements into a role-based matrix. Donors have dedicated endpoints for publishing and FCFS claim approvals; receivers use PostGIS queries to discover food and submit bounded claims; volunteers verify pickups via OTP; and administrators manage the entire platform through institutional verification queues and audit logs. All 47 endpoints enforce strict role permissions.")

    # ==================== SLIDE 17: NON-FUNCTIONAL REQUIREMENTS ====================
    slide17 = prs.slides.add_slide(blank_layout)
    add_header(slide17, "Non-Functional Architectural Requirements")
    create_card(slide17, Inches(0.8), Inches(1.6), Inches(5.6), Inches(2.5), "Security & Identity", [
        "Database-Authoritative RBAC (client claims discarded).",
        "Insecure Direct Object Reference (IDOR) protection.",
        "BCrypt-hashed 6-digit OTP (10 min expiry, 5 max tries).",
        "Immediate API lockout for suspended accounts."
    ])
    create_card(slide17, Inches(6.8), Inches(1.6), Inches(5.6), Inches(2.5), "Performance & Spatial Scalability", [
        "PostGIS GiST spatial indexing evaluates in O(log N) time.",
        "B-Tree indexing on foreign keys and status columns.",
        "Enforced Spring Data pagination on all collection APIs.",
        "Sub-millisecond query execution on proximity queries."
    ])
    create_card(slide17, Inches(0.8), Inches(4.4), Inches(5.6), Inches(2.5), "Reliability & Data Health", [
        "Spring @Transactional boundaries enforcing ACID consistency.",
        "Database check constraints prevent allocation overages.",
        "AFTER_COMMIT event isolation: notification failures cannot roll back business transactions."
    ])
    create_card(slide17, Inches(6.8), Inches(4.4), Inches(5.6), Inches(2.5), "Maintainability & Usability", [
        "Clean 3-tier Controller-Service-Repository architecture.",
        "Flyway version-controlled migrations (V1–V7).",
        "Centralized @RestControllerAdvice error handling.",
        "Role-tailored Flutter and React user interfaces."
    ])
    add_notes(slide17, "Our non-functional requirements emphasize security, performance, and transactional reliability. We avoid artificial benchmark numbers and focus on proven architectural patterns: PostGIS GiST indexing reduces spatial search to logarithmic time; database check constraints mathematically prevent over-allocation; and pessimistic locking during OTP verification eliminates concurrency race conditions.")

    # ==================== SLIDE 18: DATABASE / ER ====================
    slide18 = prs.slides.add_slide(blank_layout)
    add_header(slide18, "Database Design: Entity-Relationship Model")
    create_card(slide18, Inches(0.8), Inches(1.6), Inches(5.6), Inches(5.2), "Relational & Spatial Structure", [
        "Engine: PostgreSQL 16.8 + PostGIS 3.4.3 (7 Flyway Migrations).",
        "Core Tables (10 Normalized Entities):",
        "  • users: Central identity, Firebase UID, role, status.",
        "  • donor_profiles: 1:1 profile extension for donors.",
        "  • receiver_organizations: 1:1 profile for verified NGOs.",
        "  • volunteer_profiles: 1:1 profile with availability_status.",
        "  • donations: Food listings with GEOMETRY(Point, 4326).",
        "  • donation_requests: Bounded quantity claim requests.",
        "  • pickups: Courier dispatch, BCrypt otp_hash, timestamps.",
        "  • notifications & user_device_tokens: Alert queue.",
        "  • audit_logs: Append-only JSONB state delta logs."
    ])
    create_card(slide18, Inches(6.8), Inches(1.6), Inches(5.6), Inches(5.2), "Storage-Level Constraints & Spatial Indexing", [
        "12 CHECK Constraints Enforced:",
        "  • chk_donations_quantity_positive (> 0)",
        "  • chk_donations_allocated_quantity_non_negative (>= 0)",
        "  • chk_donations_allocated_le_quantity (allocated <= quantity)",
        "  • chk_users_role & chk_users_status enums",
        "7 UNIQUE Constraints Enforced:",
        "  • users(firebase_uid), users(email)",
        "  • 1:1 user_id constraints on extension profile tables",
        "1 Partial Unique Index: idx_unique_active_donation_request",
        "1 GiST Spatial Index: idx_donations_location_gist",
        "Coordinates stored under WGS 84 (SRID 4326)."
    ])
    add_notes(slide18, "This ER diagram shows our 10 database tables. The users table maintains central identity with 1-to-1 extension profiles for donors, receivers, and volunteers. Donations store locations natively as PostGIS geometry points with a GiST spatial index. Approved donation requests link directly to the pickups table, which stores the BCrypt-hashed OTP. Notice our append-only audit_logs table, which preserves full state traceability.")

    # ==================== SLIDE 19: CORE PROCESS FLOW ====================
    slide19 = prs.slides.add_slide(blank_layout)
    add_header(slide19, "Core Process Flow: End-to-End Redistribution")
    create_card(slide19, Inches(0.8), Inches(1.6), Inches(11.6), Inches(5.2), "10-Step Deterministic Redistribution Flow", [
        "Step 1: Donor publishes surplus listing with food safety declaration -> Status: AVAILABLE",
        "Step 2: PostGIS proximity engine indexes point; spatial query ST_DWithin locates nearby shelters.",
        "Step 3: Receiver inspects details and submits claim request -> Status: PENDING",
        "Step 4: Donor reviews claims in chronological FCFS order and approves allocation -> Status: APPROVED",
        "Step 5: System atomically increments allocated_quantity (guaranteed by DB check constraint).",
        "Step 6: Administrator assigns available volunteer courier -> Status: ASSIGNED (generates 6-digit OTP).",
        "Step 7: Plaintext OTP displayed once to admin & issued to donor; salted BCrypt hash stored in DB.",
        "Step 8: Volunteer arrives at donor facility; donor verbally provides 6-digit OTP code.",
        "Step 9: Volunteer inputs OTP via mobile; backend verifies BCrypt match under pessimistic lock -> Status: PICKED_UP",
        "Step 10: Volunteer completes delivery to shelter -> Status: COMPLETED (cascades request & donation completion; logs JSONB audit trail)."
    ])
    add_notes(slide19, "This sequence represents the core heartbeat of MealShare. A donor creates a listing with a mandatory safety declaration. The receiver discovers it via PostGIS and claims portions. The donor approves the claim, locking the quantity. The administrator assigns a volunteer, generating a 6-digit OTP. The volunteer collects the food, verifies the OTP to prove physical custody, and marks delivery complete. Every step triggers audit logs and in-app notifications.")

    # ==================== SLIDE 20: SECURITY + SAFETY ====================
    slide20 = prs.slides.add_slide(blank_layout)
    add_header(slide20, "Security & Food Safety Architecture")
    create_card(slide20, Inches(0.8), Inches(1.6), Inches(5.6), Inches(5.2), "Security Architecture", [
        "Token Authentication:",
        "  • Firebase JWT Bearer token validated by Admin SDK.",
        "Database-Authoritative RBAC:",
        "  • Roles resolved directly from PostgreSQL users table.",
        "  • Client token claims discarded to prevent tampering.",
        "Suspended Account Lockout:",
        "  • Suspended users assigned ROLE_SUSPENDED;",
        "  • Immediately isolated from operational endpoints.",
        "Cryptographic Handover Security:",
        "  • SecureRandom 6-digit numeric OTP.",
        "  • Stored exclusively as salted BCrypt hash.",
        "  • Rate-limited: max 5 failed attempts locks pickup.",
        "  • Temporal limit: 10-minute validity window.",
        "  • Pessimistic DB locking prevents race conditions."
    ])
    create_card(slide20, Inches(6.8), Inches(1.6), Inches(5.6), Inches(5.2), "Food Safety Governance", [
        "Mandatory Self-Declaration:",
        "  • Donors must explicitly agree to safety declaration",
        "    (food_safety_declared = true).",
        "Strict Temporal Validations:",
        "  • preparation_datetime <= NOW()",
        "  • consume_before > NOW()",
        "  • pickup_available_until <= consume_before",
        "Automated Query-Level Expiry Filtering:",
        "  • PostGIS queries filter consume_before > NOW(),",
        "    ensuring expired meals are never visible.",
        "Regulatory Audit Trail:",
        "  • Append-only PostgreSQL audit_logs captures actor,",
        "    action, client IP, and JSONB delta_state.",
        "Legal Reality Disclaimer:",
        "  • MealShare enforces digital operational hygiene;",
        "  • Platform does NOT certify physical food safety."
    ])
    add_notes(slide20, "Security and food safety are built directly into our application logic. On the security side, we use Firebase tokens but resolve roles strictly from PostgreSQL to prevent tampering, and we hash collection OTPs with BCrypt. On the food safety side, we enforce mandatory donor declarations, validate that preparation times are in the past and consumption limits are in the future, and automatically filter out expired meals from spatial queries.")

    # ==================== SLIDE 21: IMPLEMENTATION EVIDENCE ====================
    slide21 = prs.slides.add_slide(blank_layout)
    add_header(slide21, "Implementation Evidence (>= 50% Rubric)")
    create_card(slide21, Inches(0.8), Inches(1.6), Inches(5.6), Inches(5.2), "15 Completed Functional Subsystems", [
        "[✓] 1. Authentication & DB RBAC (UserController)",
        "[✓] 2. Donor Profile Management (DonorProfileController)",
        "[✓] 3. Surplus Food Publishing (DonorDonationController)",
        "[✓] 4. Food Safety Declarations (DonationService)",
        "[✓] 5. PostGIS Proximity Engine (ReceiverDonationController)",
        "[✓] 6. Receiver Claim Workflow (ReceiverRequestController)",
        "[✓] 7. Concurrency-Safe FCFS (DonorRequestController)",
        "[✓] 8. Volunteer Availability (VolunteerProfileRepository)",
        "[✓] 9. Admin Courier Dispatch (AdminPickupController)",
        "[✓] 10. Cryptographic OTP Auth (PickupOtpService)",
        "[✓] 11. Delivery Completion Cascading (PickupService)",
        "[✓] 12. Decoupled Notifications (NotificationEventListener)",
        "[✓] 13. Append-Only Audit Trail (AuditLogService)",
        "[✓] 14. NGO Verification Queue (AdminVerificationController)",
        "[✓] 15. Operational Admin Reports (AdminReportController)"
    ])
    create_card(slide21, Inches(6.8), Inches(1.6), Inches(5.6), Inches(5.2), "Verified Repository Deliverables", [
        "REST API Scope:",
        "  • 18 Spring Boot REST Controllers.",
        "  • 47 Documented Endpoints (OpenAPI / Swagger).",
        "Database Scope:",
        "  • 10 PostgreSQL Tables across 7 Flyway Migrations.",
        "  • PostGIS GiST Spatial Index on SRID 4326.",
        "Frontend Scope:",
        "  • React 18 Admin Web (10 management/analytics pages).",
        "  • Flutter Mobile Client (17 role-specific screens).",
        "Test Verification:",
        "  • 204 Automated Backend Tests (0 Failures).",
        "  • Clean Frontend Production Builds.",
        "Conclusion: Substantially exceeds university 50% rubric."
    ])
    add_notes(slide21, "The university guidelines require demonstrating at least 50% of project functionalities, with login counting as only 5%. MealShare far exceeds this requirement: we have implemented 15 core functional subsystems spanning 47 REST endpoints, 10 database tables, 10 React Admin pages, and 17 Flutter screens. Everything is supported by 204 automated tests with zero failures.")

    # ==================== SLIDE 22: TESTING & VALIDATION ====================
    slide22 = prs.slides.add_slide(blank_layout)
    add_header(slide22, "Automated Testing & Static Analysis Results")
    create_card(slide22, Inches(0.8), Inches(1.6), Inches(5.6), Inches(2.5), "Spring Boot Backend Test Suite", [
        "Command: .\\mvnw.cmd test (Maven Wrapper).",
        "Tests Run: 204 | Failures: 0 | Errors: 0 | Skipped: 0.",
        "Execution Time: 1 min 3 seconds.",
        "Status: BUILD SUCCESS (100% Passing)."
    ])
    create_card(slide22, Inches(6.8), Inches(1.6), Inches(5.6), Inches(2.5), "Frontend Production Builds & Linting", [
        "React 18 Admin Web (npm run build):",
        "  • 2,517 Modules Transformed | 0 TypeScript Errors.",
        "Flutter Mobile Client (flutter analyze & test):",
        "  • Static Analysis: 0 Warnings, 0 Errors.",
        "  • Unit/Widget Tests: 6/6 Passed."
    ])
    create_card(slide22, Inches(0.8), Inches(4.4), Inches(11.6), Inches(2.4), "Coverage Across Functional Security & Business Logic", [
        "[✓] Token Extraction & DB RBAC       [✓] PostGIS Spatial Radius Calculations",
        "[✓] IDOR Ownership Protection        [✓] FCFS Allocation Concurrency Safety",
        "[✓] Account Suspension Isolation     [✓] OTP BCrypt Hashing & Rate Limiting",
        "[✓] Food Safety Validation Rules     [✓] Decoupled Event Notification Pipeline",
        "[✓] Complete Lifecycle Cascades      [✓] Append-Only JSONB Audit Trail Logging"
    ])
    add_notes(slide22, "Our testing results prove the structural stability of the codebase. Our Spring Boot backend executes 204 automated tests with zero failures, covering authentication, PostGIS spatial queries, FCFS concurrency, and OTP security. Our React Admin Web builds cleanly with zero TypeScript errors across 2,500 modules, and our Flutter mobile suite passes static analysis with zero lint warnings.")

    # ==================== SLIDE 23: LIVE DEMO ====================
    slide23 = prs.slides.add_slide(blank_layout)
    add_header(slide23, "Live Demonstration Walkthrough (20 Steps)")
    create_card(slide23, Inches(0.8), Inches(1.6), Inches(11.6), Inches(5.2), "20-Step Live Demonstration Protocol (8–12 Minutes)", [
        "Phase I: Administrator Setup (Steps 1–2)",
        "  1. Admin signs into React Admin Portal -> 2. Inspect real-time KPI overview & active listing cards.",
        "Phase II: Donor Publishing & Food Safety (Steps 3–5)",
        "  3. Sign into Flutter Mobile as Donor -> 4. Publish 50 Portions Vegetable Biryani -> 5. Accept safety declaration.",
        "Phase III: Receiver Spatial Discovery & Claim (Steps 6–9)",
        "  6. Sign in as Receiver Org -> 7. PostGIS nearby radius query discovers Biryani -> 8. Inspect details -> 9. Claim 30 Portions.",
        "Phase IV: Donor FCFS Allocation Approval (Steps 10–12)",
        "  10. Donor views claims chronologically -> 11. Approves claim (atomic allocation locked: remaining = 20) -> 12. Event fires.",
        "Phase V: Admin Courier Dispatch (Step 13)",
        "  13. Admin dispatches available volunteer in Pickups page; system generates 6-digit OTP token (shown once).",
        "Phase VI: Volunteer OTP Verification & Handover (Steps 14–16)",
        "  14. Volunteer opens assigned task -> 15. Inputs 6-digit OTP code (verified via BCrypt) -> 16. Confirms delivery.",
        "Phase VII: System Verification & Reports (Steps 17–20)",
        "  17. Verify updated statuses -> 18. Inspect in-app alerts -> 19. Review JSONB audit logs -> 20. View Recharts reports."
    ])
    add_notes(slide23, "This slide summarizes our 20-step live demonstration flow, designed to run in approximately 10 minutes. We will walk through the entire lifecycle: logging in as an administrator, creating a donation as a donor, discovering it via PostGIS as a receiver, approving the allocation, dispatching a volunteer, verifying the handover via OTP, and showing the resulting audit logs and analytics. I will now switch to our live running instances.")

    # ==================== SLIDE 24: LIMITATIONS & FUTURE ====================
    slide24 = prs.slides.add_slide(blank_layout)
    add_header(slide24, "Scope Boundaries, Limitations & Future Roadmap")
    create_card(slide24, Inches(0.8), Inches(1.6), Inches(5.6), Inches(5.2), "Documented Project Scope Limitations", [
        "1. FCM Physical Device Delivery:",
        "   In-app PostgreSQL notifications verified; push delivery",
        "   to physical handsets is NOT TESTED in offline dev mode.",
        "2. Deterministic Allocation:",
        "   Allocation relies on chronological FCFS donor review;",
        "   dynamic multi-criteria automated matching is future work.",
        "3. Food Safety Liability:",
        "   Digital self-declaration; platform does not certify food.",
        "4. Live Volunteer Telemetry:",
        "   Transit tracking operates via discrete status updates;",
        "   real-time GPS streaming requires WebSocket/SSE daemon.",
        "5. Production Hardening:",
        "   Edge TLS termination and rate-limiting are future tasks."
    ])
    create_card(slide24, Inches(6.8), Inches(1.6), Inches(5.6), Inches(5.2), "Future Algorithmic Roadmap (Phase-2)", [
        "1. Automated Multi-Stop Route Optimization:",
        "   Integrate Google OR-Tools to solve the Vehicle Routing",
        "   Problem with Time Windows (VRPTW) (Reusken et al. 2024).",
        "2. Predictive Machine Learning Donor Forecasting:",
        "   Ensemble regression models (Random Forest + XGBoost)",
        "   forecasting surplus volume from historical data (Paul 2022).",
        "3. Multi-Criteria Dynamic Claim Scoring Engine.",
        "4. Production APNS / FCM Cloud Push Alert Deployment.",
        "5. Automated Background Expiry Daemon via Spring Quartz.",
        "6. Distributed Redis Caching & Kubernetes Scaling."
    ])
    add_notes(slide24, "We conclude by examining our limitations and future roadmap. In Phase-1, we focused on building a rock-solid, secure relational and spatial core. We clearly document our operational boundaries: physical FCM push alerts require production cloud certificates, and our allocation is deterministic FCFS. In Phase-2, we will leverage our clean database schema to integrate Google OR-Tools for multi-stop route optimization and train machine learning models for surplus forecasting.")

    # ==================== SLIDE 25: CONCLUSION & REFERENCES ====================
    slide25 = prs.slides.add_slide(blank_layout)
    add_header(slide25, "Conclusion & Key Academic References")
    create_card(slide25, Inches(0.8), Inches(1.6), Inches(11.6), Inches(2.2), "Capstone Engineering Conclusion", [
        "MealShare delivers a fully implemented, production-aligned software engineering solution to urban surplus food waste.",
        "Successfully unites Donors, Accredited Receivers, Volunteers, and Platform Administrators into a cohesive system.",
        "Enforces server-side PostGIS spatial proximity, concurrency-safe FCFS allocation, and cryptographic OTP handovers.",
        "Backed by 204 passing automated backend tests, clean frontend production builds, and 12 verified peer-reviewed papers."
    ])
    create_card(slide25, Inches(0.8), Inches(4.1), Inches(11.6), Inches(2.7), "Key Literature References (IEEE Style)", [
        "[1] C. Trevisan & M. Formentini, 'Digital Technologies for Food Loss Prevention...', IEEE Trans. Eng. Manage., vol. 71, 2024.",
        "[2] P. Patel & A. K. Pandey, 'An Android Application for Food Donation: Location-Based Approach,' Proc. 2023 IEEE ICACITE, 2023.",
        "[3] W. Villegas-Ch et al., 'Traceability of Perishable Products via Light Blockchain and IoT,' IEEE Access, vol. 13, 2025.",
        "[4] M. Reusken et al., 'Vehicle routing with stochastic demand for food bank collection,' Eur. J. Oper. Res., vol. 317, 2024.",
        "[5] S. Paul & L. B. Davis, 'An ensemble forecasting model for food donors,' Annals of Operations Research, vol. 319, 2022.",
        "[6] M. Damiani et al., 'Quantifying environmental implications of surplus food redistribution,' J. Cleaner Prod., vol. 289, 2021."
    ])
    add_notes(slide25, "In conclusion, MealShare delivers an authentic, full-stack engineering implementation that solves real-world coordination and logistics problems in surplus food rescue. Our work is firmly grounded in peer-reviewed literature and backed by passing automated test suites. Thank you respected evaluators and guide for your time and guidance. I am now pleased to answer your questions and proceed with the technical demonstration.")

    # Save presentation
    output_path = os.path.join("docs", "MealShare_Phase1_Presentation2.pptx")
    prs.save(output_path)
    print(f"Successfully generated PowerPoint presentation at: {output_path}")

if __name__ == "__main__":
    build_presentation()
