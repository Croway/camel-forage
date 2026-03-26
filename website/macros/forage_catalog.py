"""
MkDocs macros plugin: loads forage-catalog.json and provides Jinja2 macros
to render configuration property tables in documentation pages.

Usage in Markdown:

    {{ forage_properties("Agent") }}
    {{ forage_properties("Agent", tag="COMMON") }}
    {{ forage_properties("DataSource") }}
    {{ forage_bean_properties("Agent", "Chat Model", "ollama") }}
    {{ forage_beans_table("Agent", "Chat Model") }}
"""

import json
import os

CATALOG_PATH = os.path.join(
    os.path.dirname(__file__),
    "..",
    "..",
    "forage-catalog",
    "target",
    "generated-catalog",
    "forage-catalog.json",
)


def _load_catalog():
    path = os.path.normpath(CATALOG_PATH)
    if not os.path.exists(path):
        return None
    with open(path) as f:
        return json.load(f)


def _find_factory(catalog, factory_name):
    for factory in catalog.get("factories", []):
        if factory["name"] == factory_name:
            return factory
    return None


def _render_entries_table(entries, tag=None):
    if tag:
        entries = [e for e in entries if e.get("configTag", "").upper() == tag.upper()]

    if not entries:
        return "*No properties available.*"

    common = [e for e in entries if e.get("configTag", "").upper() == "COMMON"]
    security = [e for e in entries if e.get("configTag", "").upper() == "SECURITY"]
    advanced = [e for e in entries if e.get("configTag", "").upper() == "ADVANCED"]
    other = [e for e in entries if e.get("configTag", "").upper() not in ("COMMON", "SECURITY", "ADVANCED")]

    lines = []

    if tag:
        # Single tag requested, render flat table
        _append_table(lines, entries)
    else:
        # Group by tag
        if common:
            _append_table(lines, common)
        if security:
            lines.append("")
            lines.append("**Security**")
            lines.append("")
            _append_table(lines, security)
        if advanced:
            lines.append("")
            lines.append("**Advanced**")
            lines.append("")
            _append_table(lines, advanced)
        if other:
            _append_table(lines, other)

    return "\n".join(lines)


def _append_table(lines, entries, indent=""):
    lines.append(f"{indent}| Property | Description | Type | Default | Required |")
    lines.append(f"{indent}|---|---|---|---|---|")
    for e in entries:
        name = f'`{e["name"]}`'
        desc = e.get("description", "")
        type_ = f'`{e.get("type", "string")}`'
        default = f'`{e["defaultValue"]}`' if e.get("defaultValue") else ""
        required = "Yes" if e.get("required") else ""
        lines.append(f"{indent}| {name} | {desc} | {type_} | {default} | {required} |")


def _forage_properties(factory_name, tag=None):
    catalog = _load_catalog()
    if not catalog:
        return f"*Catalog not found. Run `mvn install -DskipTests` to generate it.*"

    factory = _find_factory(catalog, factory_name)
    if not factory:
        return f"*Factory `{factory_name}` not found in catalog.*"

    entries = factory.get("configEntries", [])
    return _render_entries_table(entries, tag)


def _forage_bean_properties(factory_name, feature, bean_name):
    catalog = _load_catalog()
    if not catalog:
        return f"*Catalog not found. Run `mvn install -DskipTests` to generate it.*"

    factory = _find_factory(catalog, factory_name)
    if not factory:
        return f"*Factory `{factory_name}` not found in catalog.*"

    for fb in factory.get("beansByFeature", []):
        if fb["feature"] == feature:
            for bean in fb["beans"]:
                if bean["name"] == bean_name:
                    entries = bean.get("configEntries", [])
                    if not entries:
                        return f"*No additional properties for `{bean_name}`. Uses the factory-level properties above.*"
                    return _render_entries_table(entries)

    return f"*Bean `{bean_name}` not found under feature `{feature}`.*"


def _forage_beans_table(factory_name, feature):
    catalog = _load_catalog()
    if not catalog:
        return f"*Catalog not found. Run `mvn install -DskipTests` to generate it.*"

    factory = _find_factory(catalog, factory_name)
    if not factory:
        return f"*Factory `{factory_name}` not found in catalog.*"

    for fb in factory.get("beansByFeature", []):
        if fb["feature"] == feature:
            lines = []
            lines.append("| Name | Description |")
            lines.append("|---|---|")
            for bean in fb["beans"]:
                name = f'`{bean["name"]}`'
                desc = bean.get("description", "")
                lines.append(f"| {name} | {desc} |")
            return "\n".join(lines)

    return f"*Feature `{feature}` not found in factory `{factory_name}`.*"


def define_env(env):
    """Hook for mkdocs-macros-plugin: register macros."""
    env.macro(_forage_properties, "forage_properties")
    env.macro(_forage_bean_properties, "forage_bean_properties")
    env.macro(_forage_beans_table, "forage_beans_table")
