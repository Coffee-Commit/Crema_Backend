#!/usr/bin/env python3
"""
OpenVidu 프로젝트 ERD 생성기
JPA 엔티티를 분석하여 Entity Relationship Diagram을 생성합니다.
"""

import os
import re
from dataclasses import dataclass, field
from typing import List, Dict, Optional
import matplotlib.pyplot as plt
import matplotlib.patches as patches
from matplotlib.patches import FancyBboxPatch, ConnectionPatch
import numpy as np

@dataclass
class Field:
    name: str
    type: str
    nullable: bool = True
    unique: bool = False
    primary_key: bool = False
    foreign_key: Optional[str] = None
    relationship_type: Optional[str] = None
    mapped_by: Optional[str] = None

@dataclass
class Entity:
    name: str
    table_name: str
    fields: List[Field] = field(default_factory=list)
    relationships: List[tuple] = field(default_factory=list)

class ERDGenerator:
    def __init__(self):
        self.entities: List[Entity] = []
        self.relationships: List[tuple] = []
        
    def parse_java_entity(self, file_path: str) -> Optional[Entity]:
        """Java 엔티티 파일을 파싱하여 Entity 객체 생성"""
        try:
            with open(file_path, 'r', encoding='utf-8') as f:
                content = f.read()
            
            # 클래스명 추출
            class_match = re.search(r'public class (\w+)', content)
            if not class_match:
                return None
            
            class_name = class_match.group(1)
            
            # 테이블명 추출
            table_match = re.search(r'@Table\(name = "([^"]+)"\)', content)
            table_name = table_match.group(1) if table_match else class_name.lower()
            
            entity = Entity(name=class_name, table_name=table_name)
            
            # 필드 분석
            fields_section = re.search(r'public class.*?\{(.*?)\n\s*@Builder', content, re.DOTALL)
            if fields_section:
                fields_content = fields_section.group(1)
                
                # 각 필드 분석
                field_pattern = r'(@[^\n]*\n)*\s*private\s+(\w+(?:<[^>]+>)?)\s+(\w+);'
                fields = re.findall(field_pattern, fields_content, re.MULTILINE)
                
                for annotations, field_type, field_name in fields:
                    field_obj = Field(name=field_name, type=field_type)
                    
                    # 어노테이션 분석
                    if '@Id' in annotations:
                        field_obj.primary_key = True
                    
                    if '@GeneratedValue' in annotations:
                        field_obj.type = f"{field_type} (AUTO)"
                    
                    # Column 어노테이션 분석
                    column_match = re.search(r'@Column\([^)]*\)', annotations)
                    if column_match:
                        column_annotation = column_match.group(0)
                        if 'nullable = false' in column_annotation:
                            field_obj.nullable = False
                        if 'unique = true' in column_annotation:
                            field_obj.unique = True
                    
                    # 관계 어노테이션 분석
                    if '@OneToMany' in annotations:
                        field_obj.relationship_type = 'OneToMany'
                        mapped_by_match = re.search(r'mappedBy = "([^"]+)"', annotations)
                        if mapped_by_match:
                            field_obj.mapped_by = mapped_by_match.group(1)
                    elif '@ManyToOne' in annotations:
                        field_obj.relationship_type = 'ManyToOne'
                    elif '@OneToOne' in annotations:
                        field_obj.relationship_type = 'OneToOne'
                    elif '@ManyToMany' in annotations:
                        field_obj.relationship_type = 'ManyToMany'
                    
                    # JoinColumn 분석
                    join_column_match = re.search(r'@JoinColumn\(name = "([^"]+)"\)', annotations)
                    if join_column_match:
                        field_obj.foreign_key = join_column_match.group(1)
                    
                    entity.fields.append(field_obj)
            
            return entity
            
        except Exception as e:
            print(f"Error parsing {file_path}: {e}")
            return None
    
    def scan_entities(self, base_path: str):
        """지정된 경로에서 모든 엔티티 파일 스캔"""
        entity_files = []
        for root, dirs, files in os.walk(base_path):
            if 'entity' in root:
                for file in files:
                    if file.endswith('.java'):
                        entity_files.append(os.path.join(root, file))
        
        for file_path in entity_files:
            entity = self.parse_java_entity(file_path)
            if entity:
                self.entities.append(entity)
                print(f"✅ Parsed entity: {entity.name} ({entity.table_name})")
    
    def analyze_relationships(self):
        """엔티티 간 관계 분석"""
        for entity in self.entities:
            for field in entity.fields:
                if field.relationship_type:
                    # 관련 엔티티 찾기
                    target_entity_name = field.type.replace('List<', '').replace('>', '')
                    target_entity = next((e for e in self.entities if e.name == target_entity_name), None)
                    
                    if target_entity:
                        if field.relationship_type == 'OneToMany':
                            relationship = (entity.name, target_entity.name, '1', '*', field.name)
                        elif field.relationship_type == 'ManyToOne':
                            relationship = (entity.name, target_entity.name, '*', '1', field.name)
                        elif field.relationship_type == 'OneToOne':
                            relationship = (entity.name, target_entity.name, '1', '1', field.name)
                        elif field.relationship_type == 'ManyToMany':
                            relationship = (entity.name, target_entity.name, '*', '*', field.name)
                        
                        self.relationships.append(relationship)
    
    def generate_mermaid_erd(self) -> str:
        """Mermaid ERD 다이어그램 생성"""
        mermaid_code = ["erDiagram"]
        
        # 엔티티 정의
        for entity in self.entities:
            mermaid_code.append(f"    {entity.table_name.upper()} {{")
            
            for field in entity.fields:
                # 타입 매핑
                type_mapping = {
                    'Long': 'BIGINT',
                    'String': 'VARCHAR',
                    'LocalDateTime': 'TIMESTAMP',
                    'Boolean': 'BOOLEAN',
                    'Integer': 'INT'
                }
                
                field_type = type_mapping.get(field.type.split('(')[0], field.type)
                
                # 제약조건 표시
                constraints = []
                if field.primary_key:
                    constraints.append('PK')
                if not field.nullable:
                    constraints.append('NOT NULL')
                if field.unique:
                    constraints.append('UNIQUE')
                if field.foreign_key:
                    constraints.append('FK')
                
                constraint_str = f" {' '.join(constraints)}" if constraints else ""
                mermaid_code.append(f"        {field_type} {field.name}{constraint_str}")
            
            mermaid_code.append("    }")
        
        # 관계 정의
        for rel in self.relationships:
            source, target, source_card, target_card, field_name = rel
            source_table = next(e.table_name.upper() for e in self.entities if e.name == source)
            target_table = next(e.table_name.upper() for e in self.entities if e.name == target)
            
            if source_card == '1' and target_card == '*':
                mermaid_code.append(f"    {source_table} ||--o{{ {target_table} : {field_name}")
            elif source_card == '*' and target_card == '1':
                mermaid_code.append(f"    {source_table} }}o--|| {target_table} : {field_name}")
            elif source_card == '1' and target_card == '1':
                mermaid_code.append(f"    {source_table} ||--|| {target_table} : {field_name}")
            elif source_card == '*' and target_card == '*':
                mermaid_code.append(f"    {source_table} }}o--o{{ {target_table} : {field_name}")
        
        return '\n'.join(mermaid_code)
    
    def generate_visual_erd(self, save_path: str = None):
        """matplotlib을 사용한 시각적 ERD 생성"""
        fig, ax = plt.subplots(1, 1, figsize=(14, 10))
        ax.set_xlim(0, 10)
        ax.set_ylim(0, 8)
        ax.axis('off')
        
        # 색상 설정
        colors = {
            'entity_bg': '#E8F4FD',
            'entity_border': '#2E86AB',
            'pk_bg': '#FFE4E1',
            'fk_bg': '#E1FFE1',
            'field_text': '#333333',
            'title_text': '#1F4E79'
        }
        
        # 엔티티 위치 계산
        entity_positions = {}
        x_positions = [2, 6]  # VideoSession, Participant
        y_positions = [5, 5]
        
        for i, entity in enumerate(self.entities):
            entity_positions[entity.name] = (x_positions[i], y_positions[i])
        
        # 엔티티 그리기
        for i, entity in enumerate(self.entities):
            x, y = entity_positions[entity.name]
            
            # 테이블 크기 계산
            field_count = len(entity.fields)
            box_height = max(2.5, field_count * 0.25 + 0.8)
            box_width = 2.5
            
            # 엔티티 박스
            entity_box = FancyBboxPatch(
                (x - box_width/2, y - box_height/2),
                box_width, box_height,
                boxstyle="round,pad=0.05",
                facecolor=colors['entity_bg'],
                edgecolor=colors['entity_border'],
                linewidth=2
            )
            ax.add_patch(entity_box)
            
            # 엔티티 제목
            ax.text(x, y + box_height/2 - 0.2, entity.table_name.upper(), 
                   ha='center', va='center', fontsize=12, fontweight='bold',
                   color=colors['title_text'])
            
            # 구분선
            ax.plot([x - box_width/2 + 0.1, x + box_width/2 - 0.1], 
                   [y + box_height/2 - 0.4, y + box_height/2 - 0.4], 
                   color=colors['entity_border'], linewidth=1)
            
            # 필드 목록
            field_y = y + box_height/2 - 0.7
            for field in entity.fields:
                # 필드 배경색 (PK, FK 구분)
                if field.primary_key:
                    field_bg = colors['pk_bg']
                    prefix = "🔑 "
                elif field.foreign_key or field.relationship_type == 'ManyToOne':
                    field_bg = colors['fk_bg']
                    prefix = "🔗 "
                else:
                    field_bg = None
                    prefix = ""
                
                if field_bg:
                    field_rect = patches.Rectangle(
                        (x - box_width/2 + 0.05, field_y - 0.08),
                        box_width - 0.1, 0.16,
                        facecolor=field_bg, alpha=0.7
                    )
                    ax.add_patch(field_rect)
                
                # 필드 타입 간소화
                display_type = field.type.replace('java.time.', '').replace('java.lang.', '')
                if '(' in display_type:
                    display_type = display_type.split('(')[0]
                
                # 제약조건 표시
                constraints = []
                if not field.nullable:
                    constraints.append('NOT NULL')
                if field.unique:
                    constraints.append('UNIQUE')
                
                constraint_text = f" ({', '.join(constraints)})" if constraints else ""
                
                field_text = f"{prefix}{field.name}: {display_type}{constraint_text}"
                ax.text(x - box_width/2 + 0.1, field_y, field_text, 
                       ha='left', va='center', fontsize=9,
                       color=colors['field_text'])
                
                field_y -= 0.2
        
        # 관계선 그리기
        for rel in self.relationships:
            source, target, source_card, target_card, field_name = rel
            
            if source in entity_positions and target in entity_positions:
                source_pos = entity_positions[source]
                target_pos = entity_positions[target]
                
                # 연결점 계산
                sx, sy = source_pos
                tx, ty = target_pos
                
                # 박스 경계에서 연결점 계산
                if sx < tx:  # source가 왼쪽
                    start_point = (sx + 1.25, sy)
                    end_point = (tx - 1.25, ty)
                else:  # source가 오른쪽
                    start_point = (sx - 1.25, sy)
                    end_point = (tx + 1.25, ty)
                
                # 관계선 그리기
                connection = ConnectionPatch(
                    start_point, end_point, "data", "data",
                    arrowstyle="->", shrinkA=0, shrinkB=0,
                    mutation_scale=20, fc=colors['entity_border'], 
                    ec=colors['entity_border'], linewidth=2
                )
                ax.add_patch(connection)
                
                # 카디널리티 표시
                mid_x = (start_point[0] + end_point[0]) / 2
                mid_y = (start_point[1] + end_point[1]) / 2 + 0.1
                
                cardinality_text = f"{source_card} : {target_card}"
                ax.text(mid_x, mid_y, cardinality_text, 
                       ha='center', va='center', fontsize=8,
                       bbox=dict(boxstyle="round,pad=0.2", facecolor='white', alpha=0.8))
                
                # 관계명 표시
                ax.text(mid_x, mid_y - 0.2, field_name, 
                       ha='center', va='center', fontsize=7, style='italic',
                       color='gray')
        
        # 제목 추가
        ax.text(5, 7.5, '🎥 OpenVidu 프로젝트 ERD', 
               ha='center', va='center', fontsize=16, fontweight='bold',
               color=colors['title_text'])
        
        # 범례 추가
        legend_elements = [
            patches.Patch(color=colors['pk_bg'], label='🔑 Primary Key'),
            patches.Patch(color=colors['fk_bg'], label='🔗 Foreign Key'),
            patches.Patch(color=colors['entity_bg'], label='📊 Entity')
        ]
        ax.legend(handles=legend_elements, loc='upper right', bbox_to_anchor=(0.98, 0.98))
        
        plt.tight_layout()
        
        if save_path:
            plt.savefig(save_path, dpi=300, bbox_inches='tight', 
                       facecolor='white', edgecolor='none')
            print(f"✅ ERD 이미지 저장됨: {save_path}")
        
        return fig
    
    def generate_report(self) -> str:
        """엔티티 분석 리포트 생성"""
        report = ["# 🎥 OpenVidu 프로젝트 데이터베이스 ERD 분석 리포트\n"]
        
        report.append(f"## 📊 전체 개요")
        report.append(f"- **총 엔티티 수**: {len(self.entities)}")
        report.append(f"- **총 관계 수**: {len(self.relationships)}")
        report.append(f"- **생성 시간**: {plt.datetime.datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n")
        
        report.append("## 🏗️ 엔티티 상세 정보\n")
        
        for entity in self.entities:
            report.append(f"### 📋 {entity.name} ({entity.table_name})")
            report.append(f"**테이블명**: `{entity.table_name}`\n")
            
            # 필드 정보를 테이블로 정리
            report.append("| 필드명 | 타입 | 제약조건 | 설명 |")
            report.append("|--------|------|----------|------|")
            
            for field in entity.fields:
                constraints = []
                if field.primary_key:
                    constraints.append("PK")
                if not field.nullable:
                    constraints.append("NOT NULL")
                if field.unique:
                    constraints.append("UNIQUE")
                if field.foreign_key:
                    constraints.append("FK")
                if field.relationship_type:
                    constraints.append(field.relationship_type)
                
                constraint_str = ", ".join(constraints) if constraints else "-"
                
                description = ""
                if field.primary_key:
                    description = "기본키"
                elif field.foreign_key:
                    description = "외래키"
                elif field.relationship_type:
                    description = f"{field.relationship_type} 관계"
                else:
                    description = "-"
                
                report.append(f"| `{field.name}` | {field.type} | {constraint_str} | {description} |")
            
            report.append("")
        
        if self.relationships:
            report.append("## 🔗 엔티티 관계\n")
            for rel in self.relationships:
                source, target, source_card, target_card, field_name = rel
                report.append(f"- **{source}** {source_card}:{target_card} **{target}** (`{field_name}`)")
        
        return '\n'.join(report)

def main():
    """메인 실행 함수"""
    print("🎥 OpenVidu 프로젝트 ERD 생성기")
    print("=" * 50)
    
    # 프로젝트 경로 설정
    project_path = "F:/코딩/Spring/Crema/src/main/java"
    output_dir = "F:/코딩/Spring/Crema/docs/erd"
    
    # 출력 디렉토리 생성
    os.makedirs(output_dir, exist_ok=True)
    
    # ERD 생성기 초기화
    erd_generator = ERDGenerator()
    
    # 엔티티 스캔
    print("📂 엔티티 파일 스캔 중...")
    erd_generator.scan_entities(project_path)
    
    if not erd_generator.entities:
        print("❌ 엔티티를 찾을 수 없습니다.")
        return
    
    # 관계 분석
    print("🔍 엔티티 관계 분석 중...")
    erd_generator.analyze_relationships()
    
    # Mermaid ERD 생성
    print("🎨 Mermaid ERD 생성 중...")
    mermaid_erd = erd_generator.generate_mermaid_erd()
    with open(f"{output_dir}/erd_mermaid.md", 'w', encoding='utf-8') as f:
        f.write("# OpenVidu ERD (Mermaid)\n\n```mermaid\n")
        f.write(mermaid_erd)
        f.write("\n```")
    print(f"✅ Mermaid ERD 저장됨: {output_dir}/erd_mermaid.md")
    
    # 시각적 ERD 생성
    print("🖼️ 시각적 ERD 생성 중...")
    try:
        fig = erd_generator.generate_visual_erd(f"{output_dir}/erd_visual.png")
        plt.show()  # ERD 화면에 표시
    except Exception as e:
        print(f"⚠️ 시각적 ERD 생성 실패: {e}")
        print("matplotlib 설치: pip install matplotlib")
    
    # 분석 리포트 생성
    print("📄 분석 리포트 생성 중...")
    report = erd_generator.generate_report()
    with open(f"{output_dir}/erd_report.md", 'w', encoding='utf-8') as f:
        f.write(report)
    print(f"✅ 분석 리포트 저장됨: {output_dir}/erd_report.md")
    
    print("\n🎉 ERD 생성 완료!")
    print(f"📁 출력 파일들:")
    print(f"   - Mermaid ERD: {output_dir}/erd_mermaid.md")
    print(f"   - 시각적 ERD: {output_dir}/erd_visual.png")
    print(f"   - 분석 리포트: {output_dir}/erd_report.md")

if __name__ == "__main__":
    try:
        import matplotlib.pyplot as plt
        import matplotlib.patches as patches
        from matplotlib.patches import FancyBboxPatch, ConnectionPatch
        import numpy as np
        
        main()
    except ImportError as e:
        print("❌ 필요한 패키지를 설치해주세요:")
        print("pip install matplotlib numpy")
        print(f"오류: {e}")