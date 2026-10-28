package com.sochi.fieldservice.config;

import com.sochi.fieldservice.engineer.entity.AvailabilityStatus;
import com.sochi.fieldservice.engineer.entity.Engineer;
import com.sochi.fieldservice.engineer.repository.EngineerRepository;
import com.sochi.fieldservice.incident.entity.Incident;
import com.sochi.fieldservice.incident.entity.IncidentHistory;
import com.sochi.fieldservice.incident.entity.IncidentPriority;
import com.sochi.fieldservice.incident.entity.IncidentStatus;
import com.sochi.fieldservice.incident.repository.IncidentHistoryRepository;
import com.sochi.fieldservice.incident.repository.IncidentRepository;
import com.sochi.fieldservice.user.entity.Role;
import com.sochi.fieldservice.user.entity.User;
import com.sochi.fieldservice.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Seeds realistic demonstration data for development environments.
 * Runs only under the 'dev' profile and ensures idempotency.
 */
@Component
@Profile("dev")
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final EngineerRepository engineerRepository;
    private final IncidentRepository incidentRepository;
    private final IncidentHistoryRepository historyRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.enabled:true}")
    private boolean seedEnabled = true;

    @Value("${app.seed.default-password:password123}")
    private String defaultPassword = "password123";

    public DataSeeder(UserRepository userRepository,
                      EngineerRepository engineerRepository,
                      IncidentRepository incidentRepository,
                      IncidentHistoryRepository historyRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.engineerRepository = engineerRepository;
        this.incidentRepository = incidentRepository;
        this.historyRepository = historyRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public DataSeeder(UserRepository userRepository,
                      EngineerRepository engineerRepository,
                      IncidentRepository incidentRepository,
                      IncidentHistoryRepository historyRepository,
                      PasswordEncoder passwordEncoder,
                      boolean seedEnabled,
                      String defaultPassword) {
        this.userRepository = userRepository;
        this.engineerRepository = engineerRepository;
        this.incidentRepository = incidentRepository;
        this.historyRepository = historyRepository;
        this.passwordEncoder = passwordEncoder;
        this.seedEnabled = seedEnabled;
        this.defaultPassword = defaultPassword;
    }

    public void setSeedEnabled(boolean seedEnabled) {
        this.seedEnabled = seedEnabled;
    }

    public void setDefaultPassword(String defaultPassword) {
        this.defaultPassword = defaultPassword;
    }

    public boolean isSeedEnabled() {
        return seedEnabled;
    }

    public String getDefaultPassword() {
        return defaultPassword;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!seedEnabled) {
            log.info("Demo data seeding is disabled via app.seed.enabled=false");
            return;
        }

        log.info("Starting demo data seeding for 'dev' profile...");
        List<Engineer> engineers = seedEngineers();
        seedUsers(engineers);
        seedIncidents(engineers);
        log.info("Demo data seeding completed successfully.");
    }

    private void seedUsers(List<Engineer> engineers) {
        if (userRepository.findByUsername("admin").isEmpty()) {
            User admin = new User("admin", "admin@telecom.net", passwordEncoder.encode(defaultPassword), Role.ROLE_ADMIN);
            userRepository.save(admin);
            log.info("Seeded user 'admin' (ROLE_ADMIN)");
        }

        if (userRepository.findByUsername("dispatcher").isEmpty()) {
            User dispatcher = new User("dispatcher", "dispatcher@telecom.net", passwordEncoder.encode(defaultPassword), Role.ROLE_MANAGER);
            userRepository.save(dispatcher);
            log.info("Seeded user 'dispatcher' (ROLE_MANAGER)");
        }

        if (userRepository.findByUsername("technician").isEmpty()) {
            Engineer primaryEngineer = (engineers != null && !engineers.isEmpty()) ? engineers.get(0) : null;
            User technician = new User("technician", "technician@telecom.net", passwordEncoder.encode(defaultPassword), Role.ROLE_ENGINEER, primaryEngineer);
            userRepository.save(technician);
            log.info("Seeded user 'technician' (ROLE_ENGINEER) linked to engineer {}", primaryEngineer != null ? primaryEngineer.getName() : "none");
        }

        if (userRepository.findByUsername("viewer").isEmpty()) {
            User viewer = new User("viewer", "viewer@telecom.net", passwordEncoder.encode(defaultPassword), Role.ROLE_VIEWER);
            userRepository.save(viewer);
            log.info("Seeded user 'viewer' (ROLE_VIEWER)");
        }
    }

    private List<Engineer> seedEngineers() {
        if (engineerRepository.count() > 0) {
            log.info("Engineers already exist (count: {}), skipping engineer seeding.", engineerRepository.count());
            return engineerRepository.findAll();
        }

        List<Engineer> engineers = new ArrayList<>();
        engineers.add(createEngineer("Rajesh Sharma", "rajesh.sharma@telecom.net", "Fibre", "+91-98201-11223", AvailabilityStatus.AVAILABLE));
        engineers.add(createEngineer("Priya Patel", "priya.patel@telecom.net", "Router", "+91-98202-22334", AvailabilityStatus.BUSY));
        engineers.add(createEngineer("Amit Kumar", "amit.kumar@telecom.net", "Wireless", "+91-98203-33445", AvailabilityStatus.AVAILABLE));
        engineers.add(createEngineer("Sunita Rao", "sunita.rao@telecom.net", "Power", "+91-98204-44556", AvailabilityStatus.AVAILABLE));
        engineers.add(createEngineer("Vikram Singh", "vikram.singh@telecom.net", "Fibre", "+91-98205-55667", AvailabilityStatus.AVAILABLE));
        engineers.add(createEngineer("Ananya Desai", "ananya.desai@telecom.net", "Router", "+91-98206-66778", AvailabilityStatus.AVAILABLE));
        engineers.add(createEngineer("Mohammed Ali", "mohammed.ali@telecom.net", "Wireless", "+91-98207-77889", AvailabilityStatus.AVAILABLE));
        engineers.add(createEngineer("Deepa Nair", "deepa.nair@telecom.net", "Power", "+91-98208-88990", AvailabilityStatus.OFF_DUTY));

        List<Engineer> saved = engineerRepository.saveAll(engineers);
        log.info("Seeded {} field engineers across Fibre, Router, Wireless, and Power specializations.", saved.size());
        return saved;
    }

    private Engineer createEngineer(String name, String email, String specialization, String phone, AvailabilityStatus availabilityStatus) {
        Engineer engineer = new Engineer();
        engineer.setName(name);
        engineer.setEmail(email);
        engineer.setSpecialization(specialization);
        engineer.setPhone(phone);
        engineer.setActive(true);
        engineer.setAvailabilityStatus(availabilityStatus);
        return engineer;
    }

    private void seedIncidents(List<Engineer> engineers) {
        if (incidentRepository.count() > 0) {
            log.info("Incidents already exist (count: {}), skipping incident seeding.", incidentRepository.count());
            return;
        }

        if (engineers == null || engineers.size() < 8) {
            engineers = engineerRepository.findAll();
            if (engineers.size() < 8) {
                log.warn("Insufficient engineers available for incident seeding. Found: {}", engineers.size());
                return;
            }
        }

        LocalDateTime now = LocalDateTime.now();

        // -------------------------------------------------------------------------
        // 8 OPEN Incidents (Unassigned)
        // -------------------------------------------------------------------------
        seedIncident("Core BGP session flap on Mumbai Gateway Router",
                "Border Gateway Protocol session flapping intermittently on peer AS13335. Packet drop observed on international transit links.",
                IncidentPriority.CRITICAL, IncidentStatus.OPEN, "Mumbai Central PoP - Rack 4B", null,
                now.minusHours(2), now.minusHours(2).plusHours(4), null, null); // Active Compliant

        seedIncident("Subsea DWDM transponder optical power failure",
                "Optical receiver reporting -28 dBm on transponder 8, exceeding BER threshold. Traffic rerouted to secondary ring.",
                IncidentPriority.CRITICAL, IncidentStatus.OPEN, "Chennai Landing Station - Room 2", null,
                now.minusHours(6), now.minusHours(2), null, null); // Active BREACHED

        seedIncident("Cell site BTS rectifier module failure",
                "Primary DC rectifier unit failed; system running on secondary rectifier with 48V float charge alarm.",
                IncidentPriority.HIGH, IncidentStatus.OPEN, "Sector 14 Tower, Bengaluru", null,
                now.minusHours(14), now.minusHours(2), null, null); // Active BREACHED

        seedIncident("Distribution switch uplink 10G SFP+ laser degraded",
                "SFP+ diagnostic DOM shows transmit power dropping below minimum threshold. High frame check sequence errors.",
                IncidentPriority.HIGH, IncidentStatus.OPEN, "Hyderabad Tech Zone - Node 7", null,
                now.minusHours(4), now.minusHours(4).plusHours(12), null, null); // Active Compliant

        seedIncident("Customer demarc switch port flapping",
                "GigabitEthernet0/0/24 experiencing continuous link down/link up transitions for enterprise client.",
                IncidentPriority.MEDIUM, IncidentStatus.OPEN, "Gurugram Cyber Hub - Building 3", null,
                now.minusHours(10), now.minusHours(10).plusHours(24), null, null); // Active Compliant

        seedIncident("SNMP polling timeout on Metro Ethernet switch",
                "NMS unable to retrieve MIB-II interface counters via SNMPv3 for edge switch cluster.",
                IncidentPriority.MEDIUM, IncidentStatus.OPEN, "Pune MIDC Hub - Floor 1", null,
                now.minusHours(26), now.minusHours(2), null, null); // Active BREACHED

        seedIncident("Scheduled firmware backup failed on distribution router",
                "Automated TFTP/SCP configuration export failed due to disk space threshold on flash memory.",
                IncidentPriority.LOW, IncidentStatus.OPEN, "Kolkata Hub - Server Room A", null,
                now.minusHours(12), now.minusHours(12).plusHours(48), null, null); // Active Compliant

        seedIncident("NTP synchronization drift on regional aggregation node",
                "PTP/NTP offset exceeded 150ms compared to stratum 1 reference clock.",
                IncidentPriority.LOW, IncidentStatus.OPEN, "Ahmedabad Aggregation Point", null,
                now.minusHours(52), now.minusHours(4), null, null); // Active BREACHED

        // -------------------------------------------------------------------------
        // 8 ASSIGNED Incidents (1 per engineer, active workload = 1)
        // -------------------------------------------------------------------------
        seedIncident("Fiber trunk attenuation surge on Ring-East",
                "OTDR trace indicates 4.2 dB optical loss at kilometer marker 14.3 on single-mode 96-core cable.",
                IncidentPriority.CRITICAL, IncidentStatus.ASSIGNED, "NH-48 Corridor Km 14.3", engineers.get(0),
                now.minusHours(1), now.minusHours(1).plusHours(4), null, null); // Active Compliant

        seedIncident("OSPF area 0 adjacency failure between core PE routers",
                "Hellos timed out on POS 1/0/0 interface. Dead timer expired causing prefix re-convergence.",
                IncidentPriority.CRITICAL, IncidentStatus.ASSIGNED, "Delhi Core PoP - Rack 12", engineers.get(1),
                now.minusHours(5), now.minusHours(1), null, null); // Active BREACHED

        seedIncident("5G gNodeB Massive MIMO sector tilt motor stuck",
                "Remote Electrical Tilt (RET) actuator alarm AISG 2.0 unresponsive on Sector 1.",
                IncidentPriority.HIGH, IncidentStatus.ASSIGNED, "Noida Sector 62 Rooftop Mast", engineers.get(2),
                now.minusHours(3), now.minusHours(3).plusHours(12), null, null); // Active Compliant

        seedIncident("Diesel generator automatic transfer switch (ATS) fault",
                "ATS failed test run exercise; utility to auxiliary power contactor coil overheating.",
                IncidentPriority.HIGH, IncidentStatus.ASSIGNED, "Jaipur Central Exchange - Substation", engineers.get(3),
                now.minusHours(15), now.minusHours(3), null, null); // Active BREACHED

        seedIncident("Micro-duct air blowing blockage during cable installation",
                "Cable blowing bead stopped at 850m distance inside 12/10mm micro-duct. Pressure drop detected.",
                IncidentPriority.MEDIUM, IncidentStatus.ASSIGNED, "Chandigarh Ring Sector 17", engineers.get(4),
                now.minusHours(8), now.minusHours(8).plusHours(24), null, null); // Active Compliant

        seedIncident("QoS policy drop rate high on SIP voice trunk",
                "Class-map VOICE-PRIORITY experiencing tail drops under peak load on upstream sub-interface.",
                IncidentPriority.MEDIUM, IncidentStatus.ASSIGNED, "Lucknow Regional Gateway", engineers.get(5),
                now.minusHours(28), now.minusHours(4), null, null); // Active BREACHED

        seedIncident("E-Band 80GHz millimeter-wave link misaligned",
                "Receive signal level (RSL) dropped from -42 dBm to -68 dBm following high wind storm.",
                IncidentPriority.LOW, IncidentStatus.ASSIGNED, "Indore South Hub Tower", engineers.get(6),
                now.minusHours(20), now.minusHours(20).plusHours(48), null, null); // Active Compliant

        seedIncident("DC plant battery bank thermal runaway alert",
                "VRLA 2V string cell 14 temperature sensor reads 44 deg C. Float charge current elevated.",
                IncidentPriority.LOW, IncidentStatus.ASSIGNED, "Kochi Marine Drive Exchange", engineers.get(7),
                now.minusHours(50), now.minusHours(2), null, null); // Active BREACHED

        // -------------------------------------------------------------------------
        // 8 IN_PROGRESS Incidents (1 per engineer, active workload = 2)
        // -------------------------------------------------------------------------
        seedIncident("Metro core splice enclosure water ingress",
                "Underground manhole flooded; fiber optic splice tray submerged causing macro-bending attenuation.",
                IncidentPriority.CRITICAL, IncidentStatus.IN_PROGRESS, "Manhole MH-221, Bandra Kurla Complex", engineers.get(0),
                now.minusMinutes(90), now.minusMinutes(90).plusHours(4), null, null); // Active Compliant

        seedIncident("MPLS Label Switched Path (LSP) fast-reroute failure",
                "RSVP-TE bypass tunnel did not signal downstream upon link outage. Secondary path desynchronized.",
                IncidentPriority.CRITICAL, IncidentStatus.IN_PROGRESS, "Bengaluru Whitefield Datacenter", engineers.get(1),
                now.minusHours(6), now.minusHours(2), null, null); // Active BREACHED

        seedIncident("Microwave backhaul radio CPRI framing sync loss",
                "Common Public Radio Interface reporting loss of synchronization to remote radio head.",
                IncidentPriority.HIGH, IncidentStatus.IN_PROGRESS, "Navi Mumbai Vashi Tower 4", engineers.get(2),
                now.minusHours(6), now.minusHours(6).plusHours(12), null, null); // Active Compliant

        seedIncident("Site inverter thyristor bridge failure on phase B",
                "48V DC to 230V AC pure sine wave inverter tripped offline. Static bypass switch engaged.",
                IncidentPriority.HIGH, IncidentStatus.IN_PROGRESS, "Surat Central Node - Basement 1", engineers.get(3),
                now.minusHours(16), now.minusHours(4), null, null); // Active BREACHED

        seedIncident("Patch panel LC connector contamination in ODF",
                "High reflectance measured (-18 dB) on ports 17-24 after fiber optic cross-connect patching.",
                IncidentPriority.MEDIUM, IncidentStatus.IN_PROGRESS, "Bhopal Main PoP - ODF Rack 2", engineers.get(4),
                now.minusHours(12), now.minusHours(12).plusHours(24), null, null); // Active Compliant

        seedIncident("ACL log buffer memory exhaustion on edge switch",
                "Deny rule logging flooded control plane buffer; SSH management sessions becoming unresponsive.",
                IncidentPriority.MEDIUM, IncidentStatus.IN_PROGRESS, "Nagpur Wardha Road Hub", engineers.get(5),
                now.minusHours(27), now.minusHours(3), null, null); // Active BREACHED

        seedIncident("Sector antenna azimuth drift due to bracket corrosion",
                "Horizontal beamwidth mispointing towards adjacent cell causing inter-cell interference (ICI).",
                IncidentPriority.LOW, IncidentStatus.IN_PROGRESS, "Visakhapatnam Beach Road Mast", engineers.get(6),
                now.minusHours(24), now.minusHours(24).plusHours(48), null, null); // Active Compliant

        seedIncident("Solar hybrid controller MPPT efficiency degradation",
                "Photovoltaic panel string voltage normal but MPPT tracker efficiency dropped below 60%.",
                IncidentPriority.LOW, IncidentStatus.IN_PROGRESS, "Jodhpur Rural Telecom Shelter", engineers.get(7),
                now.minusHours(55), now.minusHours(7), null, null); // Active BREACHED

        // -------------------------------------------------------------------------
        // 8 RESOLVED Incidents (ClosedAt is null, ResolvedAt populated)
        // -------------------------------------------------------------------------
        seedIncident("DWDM wavelength channel optical dispersion re-balanced",
                "Dispersion compensation module tuned; optical signal-to-noise ratio restored to 24 dB.",
                IncidentPriority.CRITICAL, IncidentStatus.RESOLVED, "Hyderabad Telecom Center", engineers.get(0),
                now.minusDays(3), now.minusDays(3).plusHours(4), now.minusDays(3).plusHours(2), null); // Resolved Compliant

        seedIncident("Fiber cut due to civil excavation on Highway 44",
                "Road expansion backhoe severed 48-core distribution cable. Emergency fusion splicing completed.",
                IncidentPriority.CRITICAL, IncidentStatus.RESOLVED, "NH-44 Km 88", engineers.get(1),
                now.minusDays(4), now.minusDays(4).plusHours(4), now.minusDays(4).plusHours(8), null); // Resolved BREACHED

        seedIncident("Core switch supervisor engine failover",
                "Redundant SUP-2T module assumed master role after transient memory parity interrupt.",
                IncidentPriority.HIGH, IncidentStatus.RESOLVED, "Mumbai Central DC", engineers.get(2),
                now.minusDays(2), now.minusDays(2).plusHours(12), now.minusDays(2).plusHours(6), null); // Resolved Compliant

        seedIncident("Optical time-domain reflectometer fault localization",
                "Local loop customer drop cable repaired after physical pinch behind equipment rack.",
                IncidentPriority.HIGH, IncidentStatus.RESOLVED, "Kolkata Salt Lake Sector 5", engineers.get(3),
                now.minusDays(5), now.minusDays(5).plusHours(12), now.minusDays(5).plusHours(18), null); // Resolved BREACHED

        seedIncident("Enterprise VPN tunnel IPsec phase 2 re-keying stuck",
                "Security association lifetime renegotiated and Dead Peer Detection keepalive interval adjusted.",
                IncidentPriority.MEDIUM, IncidentStatus.RESOLVED, "Gurugram DLF Phase 2", engineers.get(4),
                now.minusDays(3), now.minusDays(3).plusHours(24), now.minusDays(3).plusHours(14), null); // Resolved Compliant

        seedIncident("Cell tower air conditioning dual-unit failure",
                "Compressor capacitor replaced and refrigerant recharged to restore equipment room temperature.",
                IncidentPriority.MEDIUM, IncidentStatus.RESOLVED, "Chennai OMR IT Corridor", engineers.get(5),
                now.minusDays(6), now.minusDays(6).plusHours(24), now.minusDays(6).plusHours(32), null); // Resolved BREACHED

        seedIncident("GPS antenna receiver sync loss on LTE baseband unit",
                "Coaxial feeder cable surge protector replaced following lightning storm discharge.",
                IncidentPriority.LOW, IncidentStatus.RESOLVED, "Patna Danapur Site", engineers.get(6),
                now.minusDays(7), now.minusDays(7).plusHours(48), now.minusDays(7).plusHours(30), null); // Resolved Compliant

        seedIncident("Substation lightning protection surge arrester replacement",
                "Class I heavy-duty gas discharge tube arrester replaced after end-of-life status trip.",
                IncidentPriority.LOW, IncidentStatus.RESOLVED, "Guwahati Dispur Shelter", engineers.get(7),
                now.minusDays(8), now.minusDays(8).plusHours(48), now.minusDays(8).plusHours(60), null); // Resolved BREACHED

        // -------------------------------------------------------------------------
        // 8 CLOSED Incidents (Both ResolvedAt and ClosedAt populated)
        // -------------------------------------------------------------------------
        seedIncident("Submarine fiber repeater laser pump diode current restored",
                "Optical supervisory channel loopback test passed cleanly after laser pump stabilization.",
                IncidentPriority.CRITICAL, IncidentStatus.CLOSED, "Mumbai International Gateway", engineers.get(0),
                now.minusDays(10), now.minusDays(10).plusHours(4), now.minusDays(10).plusHours(3), now.minusDays(10).plusHours(5)); // Closed Compliant

        seedIncident("Severe optical amplifier gain tilt on DWDM C-band",
                "Dynamic gain equalizer re-calibrated across 80 optical channels.",
                IncidentPriority.CRITICAL, IncidentStatus.CLOSED, "Delhi Peeragarhi PoP", engineers.get(1),
                now.minusDays(12), now.minusDays(12).plusHours(4), now.minusDays(12).plusHours(9), now.minusDays(12).plusHours(10)); // Closed BREACHED

        seedIncident("Border router line card ASIC checksum parity error",
                "Fabric queue engine recovered after controlled line card warm reload.",
                IncidentPriority.HIGH, IncidentStatus.CLOSED, "Bengaluru Koramangala Hub", engineers.get(2),
                now.minusDays(8), now.minusDays(8).plusHours(12), now.minusDays(8).plusHours(8), now.minusDays(8).plusHours(14)); // Closed Compliant

        seedIncident("Wireless small cell backhaul fiber cable damaged",
                "Armored micro-cable pulled through conduit and re-terminated at pole distribution box.",
                IncidentPriority.HIGH, IncidentStatus.CLOSED, "Pune Hinjawadi Phase 1", engineers.get(3),
                now.minusDays(11), now.minusDays(11).plusHours(12), now.minusDays(11).plusHours(20), now.minusDays(11).plusHours(22)); // Closed BREACHED

        seedIncident("VLAN trunk negotiation mismatch on customer edge port",
                "Static 802.1Q encapsulation configured to replace dynamic DTP negotiation.",
                IncidentPriority.MEDIUM, IncidentStatus.CLOSED, "Hyderabad Gachibowli Node", engineers.get(4),
                now.minusDays(14), now.minusDays(14).plusHours(24), now.minusDays(14).plusHours(16), now.minusDays(14).plusHours(18)); // Closed Compliant

        seedIncident("Tower aviation obstruction beacon bulb burnt",
                "Dual LED medium-intensity beacon fixture installed with automatic photo-sensor switch.",
                IncidentPriority.MEDIUM, IncidentStatus.CLOSED, "Varanasi Airport Approach Mast", engineers.get(5),
                now.minusDays(15), now.minusDays(15).plusHours(24), now.minusDays(15).plusHours(36), now.minusDays(15).plusHours(38)); // Closed BREACHED

        seedIncident("Telecom rack dust filter cleaning and airflow balancing",
                "Rack intake air filters replaced and blanking panels installed to prevent hot-air recirculation.",
                IncidentPriority.LOW, IncidentStatus.CLOSED, "Chandigarh Industrial Area", engineers.get(6),
                now.minusDays(20), now.minusDays(20).plusHours(48), now.minusDays(20).plusHours(36), now.minusDays(20).plusHours(40)); // Closed Compliant

        seedIncident("Grounding busbar earth pit resistance measurement out of spec",
                "Copper chemical earth electrode treated with bentonite compound; earth resistance lowered to 0.8 ohm.",
                IncidentPriority.LOW, IncidentStatus.CLOSED, "Jaipur Mansarovar Exchange", engineers.get(7),
                now.minusDays(22), now.minusDays(22).plusHours(48), now.minusDays(22).plusHours(70), now.minusDays(22).plusHours(72)); // Closed BREACHED

        log.info("Seeded 40 realistic network incidents with SLA compliance and breach history.");
    }

    private void seedIncident(String title,
                               String description,
                               IncidentPriority priority,
                               IncidentStatus status,
                               String location,
                               Engineer assignedEngineer,
                               LocalDateTime createdAt,
                               LocalDateTime slaDeadline,
                               LocalDateTime resolvedAt,
                               LocalDateTime closedAt) {
        Incident incident = new Incident();
        incident.setTitle(title);
        incident.setDescription(description);
        incident.setPriority(priority);
        incident.setStatus(status);
        incident.setLocation(location);
        incident.setAssignedEngineer(assignedEngineer);
        incident.setCreatedAt(createdAt);
        incident.setUpdatedAt(resolvedAt != null ? resolvedAt : (closedAt != null ? closedAt : createdAt));
        incident.setSlaDeadline(slaDeadline);
        incident.setResolvedAt(resolvedAt);
        incident.setClosedAt(closedAt);

        Incident saved = incidentRepository.save(incident);

        // Record audit trail history
        historyRepository.save(new IncidentHistory(saved, "CREATED", "Incident reported: " + title));

        if (assignedEngineer != null) {
            historyRepository.save(new IncidentHistory(saved, "ASSIGNED", "Assigned to engineer: " + assignedEngineer.getName()));
        }

        if (status == IncidentStatus.IN_PROGRESS || status == IncidentStatus.RESOLVED || status == IncidentStatus.CLOSED) {
            historyRepository.save(new IncidentHistory(saved, "STATUS_CHANGED", "Moved to IN_PROGRESS"));
        }

        if (status == IncidentStatus.RESOLVED || status == IncidentStatus.CLOSED) {
            historyRepository.save(new IncidentHistory(saved, "STATUS_CHANGED", "Moved to RESOLVED"));
        }

        if (status == IncidentStatus.CLOSED) {
            historyRepository.save(new IncidentHistory(saved, "STATUS_CHANGED", "Moved to CLOSED"));
        }
    }
}
